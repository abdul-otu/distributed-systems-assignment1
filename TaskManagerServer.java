import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

public class TaskManagerServer {
    private static List<String> tasks = new ArrayList<>();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(12000)) {
            System.out.println("Task Manager Server is running. Waiting for clients...");

            while (true) {
                // Wait for a client to connect
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected: " + clientSocket.getInetAddress());

                // Create a new thread for the client
                Thread clientThread = new Thread(new ClientHandler(clientSocket));
                clientThread.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // This class handles communication with a single client
    static class ClientHandler implements Runnable {
        private final Socket clientSocket;
        private final BufferedReader in;
        private final PrintWriter out;

        // Create a handler thread
        public ClientHandler(Socket socket) throws IOException {
            this.clientSocket = socket;
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.out = new PrintWriter(socket.getOutputStream(), true);
        }

        // Handle the client request
        @Override
        public void run() {
            try {

                // Read the client request
                String clientRequest;
                while ((clientRequest = in.readLine()) != null) {
                    System.out.println("Received: " + clientRequest);

                    // Process the client request
                    if (clientRequest.equals("create")) {

                        // Read the task from the client and add it to the list
                        String task = in.readLine();
                        tasks.add(task);
                        out.println("Task created: " + task);
                        saveTasksToFile();
                    } else if (clientRequest.equals("view")) {

                        // Sort the tasks by date and time
                        tasks.sort((task1, task2) -> {
                            String[] parts1 = task1.split(" ");
                            String[] parts2 = task2.split(" ");

                            // Convert date and time components to integers
                            int year1 = Integer.parseInt(parts1[1].split("-")[0]);
                            int month1 = Integer.parseInt(parts1[1].split("-")[1]);
                            int day1 = Integer.parseInt(parts1[1].split("-")[2]);
                            int hour1 = Integer.parseInt(parts1[2].split(":")[0]);
                            int minute1 = Integer.parseInt(parts1[2].split(":")[1]);

                            int year2 = Integer.parseInt(parts2[1].split("-")[0]);
                            int month2 = Integer.parseInt(parts2[1].split("-")[1]);
                            int day2 = Integer.parseInt(parts2[1].split("-")[2]);
                            int hour2 = Integer.parseInt(parts2[2].split(":")[0]);
                            int minute2 = Integer.parseInt(parts2[2].split(":")[1]);

                            // Compare the date and time components of the two tasks
                            if (year1 != year2) {
                                return Integer.compare(year1, year2);
                            } else if (month1 != month2) {
                                return String.format("%02d", month1).compareTo(String.format("%02d", month2));
                            } else if (day1 != day2) {
                                return Integer.compare(day1, day2);
                            } else if (hour1 != hour2) {
                                return Integer.compare(hour1, hour2);
                            } else {
                                return Integer.compare(minute1, minute2);
                            }
                        });

                        // Send the tasks to the client
                        out.println("Tasks:");
                        for (String task : tasks) {
                            out.println(task);
                        }
                        out.println();
                    } else if (clientRequest.equals("delete")) {

                        // Send the tasks to the client
                        out.println("Tasks:");
                        for (int i = 0; i < tasks.size(); i++) {
                            out.println((i + 1) + ". " + tasks.get(i));
                        }
                        out.println("0. Cancel (do not delete)");
                        out.println("Enter the index of the task to delete:");

                        // Read the index of the task to delete
                        String inputIndex = in.readLine();
                        int index = Integer.parseInt(inputIndex) - 1;

                        // Delete the task if the index is valid
                        if (index >= 0 && index < tasks.size()) {
                            String deletedTask = tasks.remove(index);
                            out.println("Task deleted: " + deletedTask);
                            saveTasksToFile();
                        } else if (index == -1) {
                            out.println("Deletion canceled");
                        } else {
                            out.println("Invalid index. No task deleted.");
                        }
                        out.println("Deletion complete.");
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    // Close the client socket
                    clientSocket.close();
                    System.out.println("Client disconnected: " + clientSocket.getInetAddress());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        // Save the tasks to a file
        private void saveTasksToFile() {

            // Write the tasks to the file
            try (PrintWriter writer = new PrintWriter(new FileWriter("tasks.txt"))) {
                for (String task : tasks) {
                    writer.println(task);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
