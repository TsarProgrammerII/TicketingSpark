/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package ticketingspark;

import static spark.Spark.*;
import spark.ModelAndView;
import spark.*;

import com.google.firebase.auth.*;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.cloud.FirestoreClient;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;


import java.util.*;
import java.io.*;
import java.util.concurrent.ExecutionException;
import java.time.LocalDate;
import java.net.URLEncoder;


import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class App {
    public static void main(String[] args) {
        try {
        FileInputStream serviceAccount = new FileInputStream("src/main/resources/cofc-ticketing-system-firebase-adminsdk-6hbbf-78d8cdd1d2.json");
        FirebaseOptions options = new FirebaseOptions.Builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .build();
        FirebaseApp.initializeApp(options);
        } catch(IOException e) {
            System.err.println("Something fucked up: " + e.getMessage());
        }

        staticFiles.externalLocation("src/main/resources");

        get("/login", (req, res) -> {
            return new FileInputStream("src/main/resources/login.html");
        });

        get("/signIn", (req, res) -> {
            return new FileInputStream("src/main/resources/signIn.html");
        });

        get("/home", (req, res) -> {
            
            Map<String, String> allEvents = getAllEvents();
        
            
            res.type("text/html");
            try {
                FileInputStream fis = new FileInputStream("src/main/resources/home.html");
                String homePage = new String(fis.readAllBytes(), StandardCharsets.UTF_8);
        
                
                StringBuilder eventsListHtml = new StringBuilder("<ul>");
                for (Map.Entry<String, String> entry : allEvents.entrySet()) {
                    eventsListHtml.append("<li><a href=\"").append(entry.getValue()).append("\">")
                            .append(entry.getKey()).append("</a></li>");
                }
                eventsListHtml.append("</ul>");
        
                
                homePage = homePage.replace("<div id=\"allEvents\"></div>", eventsListHtml.toString());
        
                return homePage;
            } catch (IOException e) {
                e.printStackTrace();
                return "Error rendering home page";
            }
        });

        get("/event", (req, res) -> {
            String eventName = req.queryParams("eventName");
        
            
            String eventDescription = getEventDescription(eventName);
            String eventPrice = getEventPrice(eventName);
        
            
            res.type("text/html");
            try (FileInputStream fis = new FileInputStream("src/main/resources/eventDetails.html")) {
                String eventDetailsPage = new String(fis.readAllBytes(), StandardCharsets.UTF_8);
                eventDetailsPage = eventDetailsPage.replace("${eventName}", eventName)
                                                   .replace("${eventDescription}", eventDescription)
                                                   .replace("${eventPrice}", eventPrice);
                return eventDetailsPage;
            } catch (IOException e) {
                e.printStackTrace();
                return "Error rendering event details page";
            }
        });
        
        post("/purchase", (req, res) -> {
            String eventName = req.queryParams("eventName");
        
            res.type("text/html");
            try (FileInputStream fis = new FileInputStream("src/main/resources/purchase.html")) {
                String purchasePage = new String(fis.readAllBytes(), StandardCharsets.UTF_8);
                purchasePage = purchasePage.replace("${eventName}", eventName);
                return purchasePage;
            } catch (IOException e) {
                e.printStackTrace();
                return "Error rendering purchase page";
            }
        });
        
        post("/confirmPurchase", (req, res) -> {
            String eventName = req.queryParams("eventName");
            String username;
            if (req.session().attribute("username") == null)
            {
                username = "User not found";
            }
            else
            {
                username = req.session().attribute("username");
            }
        
            String creditCard = req.queryParams("creditCard");
            
            int ticketQuantity = Integer.parseInt(req.queryParams("ticketQuantity"));
        
            System.out.println("Does it even make it to Here?");
            int cost = processPurchase(eventName, ticketQuantity);
            String totalCost = "" + cost;
            System.out.println("Is it still working??");
        
            res.type("text/html");
            try  {
                FileInputStream fis = new FileInputStream("src/main/resources/confirmation.html");
                String confirmationPage = new String(fis.readAllBytes(), StandardCharsets.UTF_8);
                confirmationPage = confirmationPage.replace("${username}", username);
                confirmationPage = confirmationPage.replace("${eventName}", eventName);
                confirmationPage = confirmationPage.replace("${ticketAmount}", String.valueOf(ticketQuantity));
                confirmationPage = confirmationPage.replace("${totalCost}", (String)totalCost);

                LocalDate currentDate = LocalDate.now();
                confirmationPage = confirmationPage.replace("${purchaseDate}", currentDate.toString());
                return confirmationPage;
            } catch (IOException e) {
                e.printStackTrace();
                return "Error rendering confirmation page";
            }
        });
        
    

        post("/login", (req, res) -> {
            String username = req.queryParams("username");
            String password = req.queryParams("password");

            boolean isValidCredentials = validateCredentials(username, password);

            if (isValidCredentials) {
                req.session().attribute("username", username);
                res.redirect("/home");
                return "Successful login";
            } else {
                return "Invalid login. Please try again.";
            }
        });

        post("/signup", (req, res) -> {
            String email = req.queryParams("email");
            String signupUsername = req.queryParams("signupUsername");
            String signupPassword = req.queryParams("signupPassword");
    
            boolean isUserAdded = addUser(email, signupUsername, signupPassword);
    
            if (isUserAdded) {
                res.redirect("/home");
                return "Success!";
            } else {
                return "Error creating user. Please try again.";
            }
        });

        post("/logout", (req, res) -> {
            res.redirect("/login");
        
            return null;
        });

        post("/search", new Route() {
            @Override
            public Object handle(Request req, Response res) throws Exception {
                String eventName = req.queryParams("eventName");

                List<String> searchResults = searchEvents(eventName);

                StringBuilder resultHtml = new StringBuilder("<h2>Search Results:</h2><ul>");
                for (String result : searchResults) {
                    resultHtml.append("<li><a href='/event?eventName=").append(result).append("'>").append(result).append("</a></li>");
                }
                resultHtml.append("</ul>");

                String homePageHtml = new String(Files.readAllBytes(Paths.get("src/main/resources/home.html")), StandardCharsets.UTF_8);
                homePageHtml = homePageHtml.replace("<div id=\"searchResults\"></div>", resultHtml.toString());

                return homePageHtml;
            }
        });
    
    }

    private static String renderSearchResultsPage(List<String> searchResults) {
        StringBuilder resultHtml = new StringBuilder("<h2>Search Results:</h2><ul>");

        for (String result : searchResults) {
            resultHtml.append("<li><a href='/event?eventName=").append(result).append("'>").append(result).append("</a></li>");
        }

        resultHtml.append("</ul>");
        return resultHtml.toString();
    }

    private static Map<String, String> getAllEvents() {
        Firestore db = FirestoreClient.getFirestore();
        Map<String, String> allEvents = new HashMap<>();
        try {
            QuerySnapshot querySnapshot = db.collection("Events").get().get();
    
            for (QueryDocumentSnapshot document : querySnapshot) {
                String eventName = document.getString("EventName");
                String eventLink = "/event?eventName=" + URLEncoder.encode(eventName, StandardCharsets.UTF_8.toString());
                allEvents.put(eventName, eventLink);
            }
        } catch (InterruptedException | ExecutionException | UnsupportedEncodingException e) {
            System.err.println("Error fetching all events: " + e.getMessage());
        }
        return allEvents;
    }

    private static String renderEventDetailsPage(String eventName, String eventDescription) {
        return String.format("<html><body><h1>%s</h1><p>%s</p></body></html>", eventName, eventDescription);
    }
    
    private static String renderPurchasePage(String eventName) {
        return String.format("<html><body><h1>Purchase Tickets for %s</h1></body></html>", eventName);
    }
    
    private static String renderConfirmationPage(String eventName, int ticketAmount) {
        return String.format("<html><body><h1>Confirmation</h1><p>Tickets for %s successfully purchased. %d tickets purchased.</p></body></html>", eventName, ticketAmount);
    }

     private static boolean validateCredentials(String username, String password) {
            Firestore db = FirestoreClient.getFirestore();
            try {
                QuerySnapshot querySnapshot = db.collection("Users")
                        .whereEqualTo("Username", username)
                        .whereEqualTo("Password", password)
                        .get()
                        .get();
                
                for (QueryDocumentSnapshot document : querySnapshot) {
                    String documentUsername = document.getString("Username");
                    String documentPassword = document.getString("Password");

                    if (documentUsername  != null && documentPassword != null && documentUsername.equals(username) && documentPassword.equals(password)) {
                        return true;
                    }
                }
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }

            return false;
        }

        private static boolean addUser(String email, String username, String password) {
            Firestore db = FirestoreClient.getFirestore();
            try {
                QuerySnapshot existingUsers = db.collection("Users")
                        .whereEqualTo("Username", username)
                        .get()
                        .get();
    
                if (!existingUsers.isEmpty()) {
                    return false; 
                }
    
                Map<String, Object> newUser = new HashMap<>();
                newUser.put("Email", email);
                newUser.put("Username", username);
                newUser.put("Password", password);
    
                db.collection("Users").add(newUser);
                return true;
            } catch (InterruptedException | ExecutionException e) {
                System.err.println("Error adding user: " + e.getMessage());
                return false;
            }
        }

        private static List<String> searchEvents(String eventName) {
            Firestore db = FirestoreClient.getFirestore();
            List<String> searchResults = new ArrayList<>();
            try {
                QuerySnapshot querySnapshot = db.collection("Events")
                        .whereEqualTo("EventName", eventName)
                        .get()
                        .get();
    
                for (QueryDocumentSnapshot document : querySnapshot) {
                    String eventResult = document.getString("EventName");
                    searchResults.add(eventResult);
                }
            } catch (InterruptedException | ExecutionException e) {
                System.err.println("Error searching events: " + e.getMessage());
            }
            return searchResults;
        }

        private static String getEventDescription(String eventName) {
            Firestore db = FirestoreClient.getFirestore();
            try {
                QuerySnapshot querySnapshot = db.collection("Events")
                        .whereEqualTo("EventName", eventName)
                        .limit(1) 
                        .get()
                        .get();
        
                if (!querySnapshot.isEmpty()) {
                    QueryDocumentSnapshot document = querySnapshot.getDocuments().get(0);
                    return document.getString("EventDescription");
                }
            } catch (InterruptedException | ExecutionException e) {
                System.err.println("Error getting event description: " + e.getMessage());
            }
        
            return "Event details not available";
        }

        private static String getEventPrice(String eventName) {
            Firestore db = FirestoreClient.getFirestore();
            try {
                QuerySnapshot querySnapshot = db.collection("Events")
                        .whereEqualTo("EventName", eventName)
                        .limit(1) 
                        .get()
                        .get();
        
                if (!querySnapshot.isEmpty()) {
                    
                    QueryDocumentSnapshot eventDocument = querySnapshot.getDocuments().get(0);

                    DocumentReference ticketReference = (DocumentReference)eventDocument.get("Event");
        
                    DocumentSnapshot ticketDocument = ticketReference.get().get();

                    int ticketPrice = ticketDocument.getLong("TicketPrice").intValue();

                    return ticketPrice + "";
                } else {
                    throw new RuntimeException("Event not found");
                }
            } catch (InterruptedException | ExecutionException e) {
                System.err.println("Error getting event description: " + e.getMessage());
            }
        
            return "Event details not available";
        }
        

        private static int processPurchase(String eventName, int ticketQuantity) {
            Firestore db = FirestoreClient.getFirestore();
            try {
                QuerySnapshot querySnapshot = db.collection("Events")
                        .whereEqualTo("EventName", eventName)
                        .get()
                        .get();
        
                if (!querySnapshot.isEmpty()) {
                    System.out.println(querySnapshot);
                    QueryDocumentSnapshot eventDocument = querySnapshot.getDocuments().get(0);
                    
                    System.out.println("It's gonna break");
                    
                    
                    System.out.println("It's gonna break");
                    DocumentReference ticketReference = (DocumentReference)eventDocument.get("Event");
                    System.out.println("It's gonna break");
        
                    DocumentSnapshot ticketDocument = ticketReference.get().get();
                    System.out.println("It's gonna break");
        
                    int currentTicketAmount = ticketDocument.getLong("TicketAmount").intValue();
                    int ticketPrice = ticketDocument.getLong("TicketPrice").intValue();
                    System.out.println("It's gonna break soon");
        
                    if (currentTicketAmount >= ticketQuantity) {
                        ticketReference.update("TicketAmount", currentTicketAmount - ticketQuantity);
                        return ticketQuantity*ticketPrice;
                    } else {
                        throw new RuntimeException("No more tickets available for this event");
                    }
                } else {
                    throw new RuntimeException("Event not found");
                }
            } catch (InterruptedException | ExecutionException e) {
                System.err.println("Error processing purchase: " + e.getMessage());
                return -1;
            }
        }
        
}
