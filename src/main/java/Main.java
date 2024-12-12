import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in); // Initialize once
        String pathEnv = System.getenv("PATH"); // Get the PATH environment variable
        String homeDir = System.getenv("HOME"); // Get the HOME environment variable
        String[] pathDirs = pathEnv.split(":"); // Split PATH into directories

        while (true) {
            System.out.print("$ "); // Print prompt
            String input = scanner.nextLine().trim(); // Get user input and trim leading/trailing spaces

            // Handle the exit command
            if (input.equals("exit") || input.equals("exit 0") || input.equals("0")) {
                break; // Exit the loop and terminate the shell
            } 
            // Handle the echo command
            else if (input.startsWith("echo ")) {
                String echoText = input.substring(5).trim(); // Extract the text after "echo "
                // Check if the echoText starts and ends with single or double quotes
                if (echoText.length() > 1 && 
                    ((echoText.charAt(0) == '"' && echoText.charAt(echoText.length() - 1) == '"') ||
                     (echoText.charAt(0) == '\'' && echoText.charAt(echoText.length() - 1) == '\''))) {
                    echoText = echoText.substring(1, echoText.length() - 1); // Remove surrounding quotes
                } else {
                    echoText = echoText.replaceAll("\\s+", " "); // Normalize spaces for unquoted text
                }
                System.out.println(echoText); // Print the processed text
            } 
            // Handle the pwd command
            else if (input.equals("pwd")) {
                System.out.println(System.getProperty("user.dir")); // Print the current working directory
            } 
            // Handle the cd command
            else if (input.startsWith("cd ")) {
                String path = input.substring(3).trim(); // Extract the path after "cd "
                File dir;

                if (path.equals("~")) {
                    dir = new File(homeDir); // Change to the home directory
                } else if (path.startsWith("/")) {
                    dir = new File(path); // Absolute path
                } else {
                    dir = new File(System.getProperty("user.dir"), path); // Relative path
                }

                if (dir.exists() && dir.isDirectory()) {
                    System.setProperty("user.dir", dir.getCanonicalPath()); // Change the current directory
                } else {
                    System.out.println("cd: " + path + ": No such file or directory");
                }
            } 
            // Handle the type command for built-ins
            else if (input.startsWith("type ")) {
                String command = input.substring(5).trim();
                if (command.equals("echo") || command.equals("exit") || command.equals("type") || command.equals("pwd") || command.equals("cd")) {
                    System.out.println(command + " is a shell builtin");
                } else {
                    // Search for the command in PATH directories
                    boolean found = false;
                    for (String dir : pathDirs) {
                        File file = new File(dir, command);
                        if (file.exists() && file.isFile() && file.canExecute()) {
                            System.out.println(command + " is " + file.getAbsolutePath());
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        System.out.println(command + ": not found");
                    }
                }
            } 
            // Handle the cat command
            else if (input.startsWith("cat")) {
                String[] commandParts = splitCommand(input); // Split command into parts
                if (commandParts.length > 1) {
                    StringBuilder concatenatedOutput = new StringBuilder(); // To store the combined output
                    boolean firstFile = true; // Flag to track the first file

                    for (int i = 1; i < commandParts.length; i++) {
                        // Handle quoted file paths
                        String filePath = commandParts[i];
                        if ((filePath.startsWith("\"") && filePath.endsWith("\"")) ||
                            (filePath.startsWith("'") && filePath.endsWith("'"))) {
                            filePath = filePath.substring(1, filePath.length() - 1); // Remove quotes
                        }

                        File file = new File(filePath);
                        if (file.exists() && file.isFile()) {
                            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                                String line;
                                StringBuilder fileContent = new StringBuilder();
                                while ((line = reader.readLine()) != null) {
                                    fileContent.append(line);
                                }
                                if (!firstFile) {
                                    concatenatedOutput.append(""); // Add period between files
                                }
                                concatenatedOutput.append(fileContent.toString().trim());
                                firstFile = false; // After the first file, subsequent files should have a period before them
                            } catch (Exception e) {
                                System.out.println("cat: error reading file " + filePath + ": " + e.getMessage());
                            }
                        } else {
                            System.out.println("cat: " + filePath + ": No such file or directory");
                        }
                    }
                    if (concatenatedOutput.length() > 0) {
                        System.out.println(concatenatedOutput.toString());
                    }
                } else {
                    System.out.println("cat: missing file operand");
                }
            }
            // Handle external commands
            else {
                String[] commandParts = splitCommand(input); // Handle quoted arguments
                String command = commandParts[0]; // First part is the command

                // Search for the command in PATH directories
                boolean found = false;
                for (String dir : pathDirs) {
                    File file = new File(dir, command);
                    if (file.exists() && file.isFile() && file.canExecute()) {
                        found = true;

                        try {
                            // Execute the external command with arguments
                            ProcessBuilder processBuilder = new ProcessBuilder(commandParts);
                            processBuilder.redirectErrorStream(true);
                            Process process = processBuilder.start();

                            // Capture and print the output
                            Scanner processScanner = new Scanner(process.getInputStream());
                            StringBuilder output = new StringBuilder();
                            while (processScanner.hasNextLine()) {
                                output.append(processScanner.nextLine()).append("\n");
                            }
                            processScanner.close();
                            System.out.print(output.toString()); // Print the captured output

                            // Wait for the process to complete
                            process.waitFor();
                        } catch (Exception e) {
                            System.out.println(command + ": error while executing command");
                        }
                        break;
                    }
                }
                if (!found) {
                    System.out.println(command + ": not found");
                }
            }
        }
    }

    // Helper method to split command preserving quotes
    private static String[] splitCommand(String input) {
        // Regular expression to handle spaces outside of quoted text
        return input.split(" (?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)(?=(?:[^\']*\'[^\']*\')*[^\']*$)");
    }
}

