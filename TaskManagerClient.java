import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.Socket;

public class TaskManagerClient {
    public static void main(String[] args) {

        // Set default font for all Swing components
        Font defaultFont = new Font("Arial", Font.PLAIN, 16);
        UIManager.put("Button.font", defaultFont);
        UIManager.put("Label.font", defaultFont);
        UIManager.put("TextField.font", defaultFont);
        UIManager.put("TextArea.font", defaultFont);

        // Create the GUI
        JFrame frame = new JFrame("Task Manager Client");
        JTextArea textArea = new JTextArea(20, 30);
        textArea.setEditable(false);
        frame.add(new JScrollPane(textArea), BorderLayout.CENTER);

        // Create the create button GUI component
        JButton createButton = new JButton("Create Task");
        frame.add(createButton, BorderLayout.NORTH);
        
        // Add action listeners to the create button
        createButton.addActionListener(new ActionListener() {
            @Override
            // When the create button is clicked, this method is called
            public void actionPerformed(ActionEvent e) {
                while (true) {

                    // Ask the user for the task title
                    String title = JOptionPane.showInputDialog("Enter task title:");
                    if (title == null) {
                        // User canceled, exit the loop
                        break;
                    } else if (title.trim().isEmpty()) {
                        // User entered an empty string, show an error message and continue the loop
                        JOptionPane.showMessageDialog(null, "Task name cannot be empty.");
                        continue;
                    }

                    // Ask the user for the task date and time
                    JPanel dateInputPanel = new JPanel();
                    JTextField yearField = new JTextField(3);
                    JTextField monthField = new JTextField(3);
                    JTextField dayField = new JTextField(3);
                    JTextField hourField = new JTextField(3);
                    JTextField minuteField = new JTextField(3);

                    // Add the text fields to the panel
                    dateInputPanel.add(new JLabel("YY:"));
                    dateInputPanel.add(yearField);
                    dateInputPanel.add(new JLabel("MM:"));
                    dateInputPanel.add(monthField);
                    dateInputPanel.add(new JLabel("DD:"));
                    dateInputPanel.add(dayField);
                    dateInputPanel.add(new JLabel("HH:"));
                    dateInputPanel.add(hourField);
                    dateInputPanel.add(new JLabel("MM:"));
                    dateInputPanel.add(minuteField);

                    // Show the panel in a dialog box
                    int result = JOptionPane.showConfirmDialog(null, dateInputPanel, "Enter Date", JOptionPane.OK_CANCEL_OPTION);
                    if (result != JOptionPane.OK_OPTION) {
                        // User canceled, exit the loop
                        break;
                    }

                    // Get the text from the text fields
                    String yearText = yearField.getText();
                    String monthText = monthField.getText();
                    String dayText = dayField.getText();
                    String hourText = hourField.getText();
                    String minuteText = minuteField.getText();

                    // Check if any of the text fields are empty
                    if (yearText.isEmpty() || monthText.isEmpty() || dayText.isEmpty() || hourText.isEmpty() || minuteText.isEmpty()) {
                        JOptionPane.showMessageDialog(null, "Please fill in all date fields.");
                        continue;
                    }

                    // Check if the text fields contain numeric values
                    try {
                        int year = Integer.parseInt(yearText);
                        int month = Integer.parseInt(monthText);
                        int day = Integer.parseInt(dayText);
                        int hour = Integer.parseInt(hourText);
                        int minute = Integer.parseInt(minuteText);

                        // Check if the date and time values are valid (month 1-12, day 1-31, hour 0-23, minute 0-59)
                        if (month < 1 || month > 12 || day < 1 || day > 31 || hour < 0 || hour > 23 || minute < 0 || minute > 59) {
                            JOptionPane.showMessageDialog(null, "Invalid date and time values. Please check your input.");
                            continue;
                        }

                        // Create the task string
                        String task = title + " " + year + "-" + month + "-" + day + " " + hour + ":" + minute;

                        // Check if the task already exists in the task list
                        if (taskAlreadyExists(textArea, task)) {

                            // Ask the user if they want to create a duplicate task
                            int choice = JOptionPane.showConfirmDialog(null, "This task already exists. Do you want to create a duplicate task?", "Task Already Exists", JOptionPane.YES_NO_OPTION);
                            if (choice != JOptionPane.YES_OPTION) {
                                // User chose not to create a duplicate task, continue the loop
                                continue;
                            }
                        }

                        try (Socket socket = new Socket("localhost", 12000);                        
                            // Create a BufferedReader to read the server's response and a PrintWriter to send the task
                             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

                            // Send the task to the server
                            out.println("create");
                            out.println(task);
                            refreshTaskList(textArea);
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                        break;
                    } catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog(null, "Please enter numeric values for date and time fields.");
                    }
                }
            }
        });

        // Create the delete button GUI component
        JButton deleteButton = new JButton("Delete Task");
        frame.add(deleteButton, BorderLayout.SOUTH);

        // Add action listeners to the delete button
        deleteButton.addActionListener(new ActionListener() {
            @Override
            // When the delete button is clicked, this method is called
            public void actionPerformed(ActionEvent e) {
                try (Socket socket = new Socket("localhost", 12000);

                    // Create a BufferedReader to read the server's response and a PrintWriter to send the task
                     BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                     PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
        
                    // Ask the user for the index of the task to delete
                    String indexStr = JOptionPane.showInputDialog("Enter the index of the task to delete:");
                    try {
                        // Convert the index string to an integer
                        int indexToDelete = Integer.parseInt(indexStr);

                        // Send the index to the server
                        if (indexToDelete > 0) {
                            out.println("delete");
                            out.println(indexToDelete);
        
                            // Display the new task list
                            refreshTaskList(textArea);
                        }
                    } catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog(null, "Please enter a valid index for deletion.");
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });
        

        // Set up the frame for the main window and display it
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        // Display the initial task list
        refreshTaskList(textArea);
    }

    // This method refreshes the task list by sending a "view" command to the server
    private static void refreshTaskList(JTextArea textArea) {
        try (Socket socket = new Socket("localhost", 12000);

            // Create a BufferedReader to read the server's response and a PrintWriter to send the task
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            // Send the "view" command to the server
            out.println("view");
            String task;

            // Clear the text area
            textArea.setText("");
            int taskIndex = 1;

            // Read the tasks from the server and display them in the text area
            while ((task = in.readLine()) != null) {
                if (task.isEmpty()) {
                    break;
                }

                // Skip the "Tasks:" line
                if (!task.equals("Tasks:")) {
                    textArea.append(taskIndex + ". " + task + "\n");
                    taskIndex++;
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // This method checks if a task already exists in the task list
    private static boolean taskAlreadyExists(JTextArea textArea, String taskToCheck) {
        String text = textArea.getText();
        return text.contains(taskToCheck);
    }
}
