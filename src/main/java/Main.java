import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);
        String pathEnv = System.getenv("PATH"); 
        String homeDir = System.getenv("HOME");
        String[] pathDirs = pathEnv.split(":");

        while (true) {
            System.out.print("$ ");
            String input = scanner.nextLine().trim();

            if (input.equals("exit") || input.equals("exit 0") || input.equals("0")) {
                break;
            }
            else if (input.startsWith("echo ")) {
                String echoText = input.substring(5);
                StringBuilder result = new StringBuilder();
                boolean inQuotes = false;
                char quoteType = 0;
                boolean backcount = echoText.contains("\\"+" "+"\\");
                boolean singleback = echoText.contains("\\"+"n");
                
                for (int i = 0; i < echoText.length(); i++) {
                    char c = echoText.charAt(i);
                    if((c == '\\') && !inQuotes && singleback){
                        continue;
                    }
                    if((c == '\\') && !inQuotes && backcount){
                        result.append(' ');
                        continue;
                    }
                    if ((c == '\'' || c == '"') && !inQuotes) {
                        inQuotes = true;
                        quoteType = c;
                        continue;
                    } else if (c == quoteType && inQuotes) {
                        inQuotes = false;
                        quoteType = 0;
                        continue;
                    }
                    
                    if (c == ' ' && !inQuotes) {
                        if (result.length() > 0 && result.charAt(result.length() - 1) != ' ') {
                            result.append(' ');
                        }
                    } else {
                        result.append(c);
                    }
                }
                
                System.out.println(result.toString().trim());
            }
            else if (input.equals("pwd")) {
                System.out.println(System.getProperty("user.dir"));
            }
            else if (input.startsWith("cd ")) {
                String path = input.substring(3).trim();
                File dir;

                if (path.equals("~")) {
                    dir = new File(homeDir);
                } else if (path.startsWith("/")) {
                    dir = new File(path);
                } else {
                    dir = new File(System.getProperty("user.dir"), path);
                }

                if (dir.exists() && dir.isDirectory()) {
                    System.setProperty("user.dir", dir.getCanonicalPath());
                } else {
                    System.out.println("cd: " + path + ": No such file or directory");
                }
            }
            else if (input.startsWith("type ")) {
                String command = input.substring(5).trim();
                if (command.equals("echo") || command.equals("exit") || command.equals("type") || command.equals("pwd") || command.equals("cd")) {
                    System.out.println(command + " is a shell builtin");
                } else {
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
            else if (input.startsWith("cat ")) {
                String[] commandParts = parseCommandWithQuotes(input);
                if (commandParts.length > 1) {
                    ArrayList<String> filePaths = new ArrayList<>();
                    
                    for (int i = 1; i < commandParts.length; i++) {
                        String filePath = commandParts[i];
                        // Remove surrounding quotes if present
                        if ((filePath.startsWith("'") && filePath.endsWith("'")) ||
                            (filePath.startsWith("\"") && filePath.endsWith("\""))) {
                            filePath = filePath.substring(1, filePath.length() - 1);
                        }
                        filePaths.add(filePath);
                    }

                    if (!filePaths.isEmpty()) {
                        StringBuilder result = new StringBuilder();
                        boolean firstFile = true;

                        for (String filePath : filePaths) {
                            try {
                                String content = readFile(new File(filePath));
                                if (!firstFile) {
                                    //result.append(".");
                                }
                                result.append(content);
                                firstFile = false;
                            } catch (IOException e) {
                                System.out.println("cat: " + filePath + ": No such file or directory");
                                return;  // Exit on first error
                            }
                        }

                        if (result.length() > 0) {
                            System.out.println(result.toString());
                        }
                    }
                } else {
                    System.out.println("cat: missing file operand");
                }
            }
            else {
                String[] commandParts = splitCommand(input);
                String command = commandParts[0];

                boolean found = false;
                for (String dir : pathDirs) {
                    File file = new File(dir, command);
                    if (file.exists() && file.isFile() && file.canExecute()) {
                        found = true;
                        try {
                            ProcessBuilder processBuilder = new ProcessBuilder(commandParts);
                            processBuilder.redirectErrorStream(true);
                            Process process = processBuilder.start();

                            Scanner processScanner = new Scanner(process.getInputStream());
                            StringBuilder output = new StringBuilder();
                            while (processScanner.hasNextLine()) {
                                output.append(processScanner.nextLine()).append("\n");
                            }
                            processScanner.close();
                            System.out.print(output.toString());

                            process.waitFor();
                        } catch (Exception e) {
                            System.out.println(command + ": error while executing command");
                        }
                        break;
                    }
                }
                if (!found) {
                    System.out.println(command + ": command not found");
                }
            }
        }
    }

    private static String[] parseCommandWithQuotes(String input) {
        ArrayList<String> parts = new ArrayList<>();
        StringBuilder currentPart = new StringBuilder();
        boolean inQuotes = false;
        char quoteChar = 0;
        
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            
            if (c == '\'' || c == '"') {
                if (!inQuotes) {
                    if (currentPart.length() > 0 && !Character.isWhitespace(currentPart.charAt(currentPart.length() - 1))) {
                        parts.add(currentPart.toString());
                        currentPart = new StringBuilder();
                    }
                    inQuotes = true;
                    quoteChar = c;
                    currentPart.append(c);
                } else if (c == quoteChar) {
                    currentPart.append(c);
                    parts.add(currentPart.toString());
                    currentPart = new StringBuilder();
                    inQuotes = false;
                } else {
                    currentPart.append(c);
                }
            } else if (Character.isWhitespace(c) && !inQuotes) {
                if (currentPart.length() > 0) {
                    parts.add(currentPart.toString());
                    currentPart = new StringBuilder();
                }
            } else {
                currentPart.append(c);
            }
        }
        
        if (currentPart.length() > 0) {
            parts.add(currentPart.toString());
        }
        
        return parts.toArray(new String[0]);
    }

    private static String[] splitCommand(String input) {
        ArrayList<String> parts = new ArrayList<>();
        StringBuilder currentPart = new StringBuilder();
        boolean inQuotes = false;
        char quoteType = 0;
        boolean escaped = false;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            if (escaped) {
                currentPart.append(c);
                escaped = false;
                continue;
            }

            if (c == '\\') {
                escaped = true;
                currentPart.append(c);
                continue;
            }

            if ((c == '"' || c == '\'') && (quoteType == 0 || quoteType == c)) {
                inQuotes = !inQuotes;
                quoteType = inQuotes ? c : 0;
                currentPart.append(c);
            } else if (c == ' ' && !inQuotes) {
                if (currentPart.length() > 0) {
                    parts.add(currentPart.toString());
                    currentPart.setLength(0);
                }
            } else {
                currentPart.append(c);
            }
        }

        if (currentPart.length() > 0) {
            parts.add(currentPart.toString());
        }

        return parts.toArray(new String[0]);
    }

    private static String readFile(File file) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            boolean firstLine = true;
            while ((line = reader.readLine()) != null) {
                if (!firstLine) {
                    content.append("\n");
                }
                content.append(line);
                firstLine = false;
            }
        }
        return content.toString();
    }
}
