public class Request {
    private String name;
    private String email;
    private String message;

    public Request(String name, String email, String message) {
        this.name = name;
        this.email = email;
        this.message = message;
    }

    public String name() {
        return name;
    }

    public String email() {
        return email;
    }

    public String message() {
        return message;
    }
}
