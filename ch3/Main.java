import java.io.*;

public class Main {
    public static void main(String[] args) {
        try {
            new MiniJavaParser(System.in).Goal();
            System.out.println("Parsing successful!");
        } catch (ParseException e) {
            System.out.println("Parsing Error : \n" + e.toString());
        }
    }
}
