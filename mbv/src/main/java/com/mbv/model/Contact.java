package com.mbv.model;

public class Contact {

    private String name;
    private String email;

    public Contact() {}

    public Contact(String name, String email) {
        this.name = name;
        this.email = email;
    }

    public Contact(String email) {
        this.email = email;
        this.name = extractNameFromEmail(email);
    }

    private String extractNameFromEmail(String email) {
        if (email == null || !email.contains("@")) return email;
        String local = email.substring(0, email.indexOf('@'));
        return local.replace(".", " ").replace("_", " ");
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getDisplayName() {
        if (name != null && !name.isEmpty()) {
            return name + " <" + email + ">";
        }
        return email;
    }

    public String getDomain() {
        if (email != null && email.contains("@")) {
            return email.substring(email.indexOf('@') + 1);
        }
        return "";
    }

    @Override
    public String toString() {
        return getDisplayName();
    }
}
