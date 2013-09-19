import java.util.regex.Pattern;
import java.io.*;
import java.util.*;

public class DocsMaker {

    public static ArrayList<String> readFileLines(String filename) throws FileNotFoundException {
        ArrayList<String> lines = new ArrayList<String>();
        Scanner sc = new Scanner(new File(filename));
        while(sc.hasNext()) {
            lines.add(sc.nextLine());
        }
        sc.close();
        return lines;
    }

    static class FunctionDescript {
        private String functDescript, returnDescript;
        private ArrayList<Parameter> parameters;
        private ArrayList<String> notes;

        class Parameter {
            String name, description;
            public Parameter(String n, String d) {
                name = n;
                description = d;
            }
        }
        
        public FunctionDescript(String functDescript) {
            this.functDescript = functDescript;
            parameters = new ArrayList<Parameter>();
            notes = new ArrayList<String>();
        }

        public void addParameter(String line) {
            String s = line.split("@param ")[1];
            int firstSpace = s.indexOf(' ');
            
            parameters.add(new Parameter(
                s.substring(0, firstSpace),
                s.substring(firstSpace, s.length())
                ));
        }

        public void addNote(String line) {
            notes.add(line);
        }

        public void setReturnDescript(String line) {
            returnDescript = line;
        }

        public String toString() {
            return functDescript + "\n" + 
                   parameters.toString() + "\n" +
                   returnDescript + "\n" + 
                   notes.toString() + "\n" +
                   "--\n";
        }
    }

    static class FileDescript {
        ArrayList<String> includes;
        ArrayList<FunctionDescript> fdescripts;

        public FileDescript() {
            includes = new ArrayList<String>();
            fdescripts = new ArrayList<FunctionDescript>();
        }
    
        public void addFunctDescript(FunctionDescript f) {
            fdescripts.add(f);
        }

        public void addInclude(String line) {
            includes.add(line);
        }

        public String toString() {
            return includes.toString() + "\n" + fdescripts;
        }
    }

    static Pattern includePattern = Pattern.compile(".*#include.*"),
                   functPattern = Pattern.compile("/\\*\\*.*"),
                   paramPattern = Pattern.compile(".*@param.*"),
                   returnPattern = Pattern.compile(".*@return.*"),
                   notePattern = Pattern.compile(".*Note:.*");

    public static void createOutput(ArrayList<String> lines) {
        FileDescript fileDescript = new FileDescript();
        FunctionDescript curFunct = null;

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            // if the line matches a #include line, add it to the list of includes
            if (includePattern.matcher(line).matches()) {
                fileDescript.addInclude(line);
            }
            // else if the line matches "/**", create a new FunctionDoc object for it
            else if (functPattern.matcher(line).matches()) {
                if (curFunct != null) {
                    fileDescript.addFunctDescript(curFunct);
                }
    
                curFunct = new FunctionDescript(lines.get(i+1));
            }
            // else if we're currently building a FunctionDoc object, check for 
            else if (curFunct != null) {
                // @param
                if (paramPattern.matcher(line).matches()) {
                    curFunct.addParameter(line);
                }
                // @return
                else if (returnPattern.matcher(line).matches()) {
                    curFunct.setReturnDescript(line);
                }
                // Note:
                else if (notePattern.matcher(line).matches()) {
                    curFunct.addNote(line);
                }
            }
        }
        
        if (curFunct != null) {
            fileDescript.addFunctDescript(curFunct);
        }
    
        System.out.println(fileDescript);
    }

    public static void main(String[] args) throws FileNotFoundException {
        if (args.length == 0) {
            System.out.println("yo you didn't give us any files to read, dude.");
            return;
        }

        for (String filename : args) {
            ArrayList<String> lines = readFileLines(filename);
            createOutput(lines);
        }
    }
}
