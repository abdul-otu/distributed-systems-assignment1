import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

public class TaskManagerServer {
    private static List<String> tasks = new ArrayList<>();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(12345)) {
            System.out.println("Task Manager Server is running. Waiting for clients...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected: " + clientSocket.getInetAddress());

                Thread clientThread = new Thread(new ClientHandler(clientSocket));
                clientThread.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class ClientHandler implements Runnable {
        private final Socket clientSocket;
        private final BufferedReader in;
        private final PrintWriter out;

        public ClientHandler(Socket socket) throws IOException {
            this.clientSocket = socket;
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.out = new PrintWriter(socket.getOutputStream(), true);
        }

        @Override
        public void run() {
            try {
                String clientRequest;
                while ((clientRequest = in.readLine()) != null) {
                    System.out.println("Received: " + clientRequest);

                    if (clientRequest.equals("create")) {
                        String task = in.readLine();
                        tasks.add(task);
                        out.println("Task created: " + task);
                        saveTasksToFile();
                    } else if (clientRequest.equals("view")) {
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

                            // Compare years, then formatted months (with leading zeros), then days, then hours, then minutes
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

                        out.println("Tasks:");
                        for (String task : tasks) {
                            out.println(task);
                        }
                        out.println();
                    } else if (clientRequest.equals("delete")) {
                        out.println("Tasks:");
                        for (int i = 0; i < tasks.size(); i++) {
                            out.println((i + 1) + ". " + tasks.get(i));
                        }
                        out.println("0. Cancel (do not delete)");
                        out.println("Enter the index of the task to delete:");

                        String inputIndex = in.readLine();
                        int index = Integer.parseInt(inputIndex) - 1;

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
                    clientSocket.close();
                    System.out.println("Client disconnected: " + clientSocket.getInetAddress());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void saveTasksToFile() {
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