package model;

/**
 * Abstract class representing a generic user in the TalentFlow ATS.
 * Demonstrates Abstraction, Encapsulation, and Security attributes.
 * Serves as the base class for Candidate and Recruiter.
 */
public abstract class User {
    private int id;
    private String name;
    private String email;
    private String phone;
    private String password;

    /**
     * Default Constructor
     */
    public User() {
        this.id = 0;
        this.name = "";
        this.email = "";
        this.phone = "";
        this.password = "";
    }

    /**
     * Parameterized Constructor to initialize user attributes.
     * 
     * @param id       Unique Identifier for the user
     * @param name     Full name of the user
     * @param email    Valid email address of the user
     * @param phone    10-digit contact phone number
     * @param password Secure password for role authentication
     */
    public User(int id, String name, String email, String phone, String password) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.password = password;
    }

    /**
     * Gets the user ID.
     * @return user id
     */
    public int getId() {
        return id;
    }

    /**
     * Sets the user ID.
     * @param id user id to set
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * Gets the user's name.
     * @return user name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the user's name.
     * @param name user name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the user's email address.
     * @return user email
     */
    public String getEmail() {
        return email;
    }

    /**
     * Sets the user's email address.
     * @param email user email to set
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Gets the user's phone number.
     * @return phone number
     */
    public String getPhone() {
        return phone;
    }

    /**
     * Sets the user's phone number.
     * @param phone phone number to set
     */
    public void setPhone(String phone) {
        this.phone = phone;
    }

    /**
     * Gets the user's password.
     * @return password string
     */
    public String getPassword() {
        return password;
    }

    /**
     * Sets the user's password.
     * @param password password to set
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Abstract method to display specific user details.
     * Must be implemented by subclasses Candidate and Recruiter (Polymorphism).
     */
    public abstract void displayDetails();
}
