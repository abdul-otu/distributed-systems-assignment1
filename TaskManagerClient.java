import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.Socket;

public class TaskManagerClient {
    public static void main(String[] args) {

        Font defaultFont = new Font("Arial", Font.PLAIN, 16);
        UIManager.put("Button.font", defaultFont);
        UIManager.put("Label.font", defaultFont);
        UIManager.put("TextField.font", defaultFont);
        UIManager.put("TextArea.font", defaultFont);

        JFrame frame = new JFrame("Task Manager Client");
        JTextArea textArea = new JTextArea(20, 30);
        textArea.setEditable(false);
        frame.add(new JScrollPane(textArea), BorderLayout.CENTER);

        JButton createButton = new JButton("Create Task");
        frame.add(createButton, BorderLayout.NORTH);
        

        createButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                while (true) {
                    String title = JOptionPane.showInputDialog("Enter task title:");
                    if (title == null) {
                        // User canceled, exit the loop
                        break;
                    } else if (title.trim().isEmpty()) {
                        // Task name is empty, show an error message and ask again
                        JOptionPane.showMessageDialog(null, "Task name cannot be empty.");
                        continue;
                    }

                    JPanel dateInputPanel = new JPanel();
                    JTextField yearField = new JTextField(3);
                    JTextField monthField = new JTextField(3);
                    JTextField dayField = new JTextField(3);
                    JTextField hourField = new JTextField(3);
                    JTextField minuteField = new JTextField(3);

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

                    int result = JOptionPane.showConfirmDialog(null, dateInputPanel, "Enter Date", JOptionPane.OK_CANCEL_OPTION);
                    if (result != JOptionPane.OK_OPTION) {
                        // User canceled, exit the loop
                        break;
                    }

                    String yearText = yearField.getText();
                    String monthText = monthField.getText();
                    String dayText = dayField.getText();
                    String hourText = hourField.getText();
                    String minuteText = minuteField.getText();

                    if (yearText.isEmpty() || monthText.isEmpty() || dayText.isEmpty() || hourText.isEmpty() || minuteText.isEmpty()) {
                        JOptionPane.showMessageDialog(null, "Please fill in all date fields.");
                        continue;
                    }

                    try {
                        int year = Integer.parseInt(yearText);
                        int month = Integer.parseInt(monthText);
                        int day = Integer.parseInt(dayText);
                        int hour = Integer.parseInt(hourText);
                        int minute = Integer.parseInt(minuteText);

                        if (month < 1 || month > 12 || day < 1 || day > 31 || hour < 0 || hour > 23 || minute < 0 || minute > 59) {
                            JOptionPane.showMessageDialog(null, "Invalid date and time values. Please check your input.");
                            continue;
                        }

                        String task = title + " " + year + "-" + month + "-" + day + " " + hour + ":" + minute;

                        // Check for duplicates
                        if (taskAlreadyExists(textArea, task)) {
                            int choice = JOptionPane.showConfirmDialog(null, "This task already exists. Do you want to create a duplicate task?", "Task Already Exists", JOptionPane.YES_NO_OPTION);
                            if (choice != JOptionPane.YES_OPTION) {
                                continue; // User chose not to create a duplicate
                            }
                        }

                        try (Socket socket = new Socket("localhost", 12345);
                             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

                            out.println("create");
                            out.println(task);
                            refreshTaskList(textArea);

                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }

                        break; // Successfully created the task, exit the loop
                    } catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog(null, "Please enter numeric values for date and time fields.");
                    }
                }
            }
        });

        JButton deleteButton = new JButton("Delete Task");
        frame.add(deleteButton, BorderLayout.SOUTH);

        deleteButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try (Socket socket = new Socket("localhost", 12345);
                     BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                     PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
        
                    String indexStr = JOptionPane.showInputDialog("Enter the index of the task to delete:");
                    try {
                        int indexToDelete = Integer.parseInt(indexStr);

                        if (indexToDelete > 0) {
                            out.println("delete");
                            out.println(indexToDelete);
        
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
        

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        refreshTaskList(textArea);
    }

    private static void refreshTaskList(JTextArea textArea) {
        try (Socket socket = new Socket("localhost", 12345);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            out.println("view");
            String task;
            textArea.setText(""); // Clear the text area

            int taskIndex = 1; // Initialize task index

            while ((task = in.readLine()) != null) {
                if (task.isEmpty()) {
                    break;
                }

                // Display tasks without "Tasks:"
                if (!task.equals("Tasks:")) {
                    textArea.append(taskIndex + ". " + task + "\n");
                    taskIndex++;
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static boolean taskAlreadyExists(JTextArea textArea, String taskToCheck) {
        String text = textArea.getText();
        return text.contains(taskToCheck);
    }
}