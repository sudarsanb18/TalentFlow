package model;

/**
 * Recruiter class representing HR personnel or hiring managers in TalentFlow.
 * Demonstrates Inheritance (extends User), Encapsulation, and Security.
 */
public class Recruiter extends User {
    private String company;

    /**
     * Default Constructor
     */
    public Recruiter() {
        super();
        this.company = "";
    }

    /**
     * Parameterized Constructor
     * 
     * @param id       Unique recruiter ID
     * @param name     Recruiter full name
     * @param email    Recruiter email address
     * @param phone    Recruiter phone number
     * @param password Recruiter password
     * @param company  Company name
     */
    public Recruiter(int id, String name, String email, String phone, String password, String company) {
        super(id, name, email, phone, password);
        this.company = company;
    }

    /**
     * Gets recruiter company name.
     * @return company name
     */
    public String getCompany() {
        return company;
    }

    /**
     * Sets recruiter company name.
     * @param company company name
     */
    public void setCompany(String company) {
        this.company = company;
    }

    /**
     * Implementation of abstract displayDetails method from User (Polymorphism).
     */
    @Override
    public void displayDetails() {
        System.out.println("--------------------------------------------------------------------------------");
        System.out.printf("RECRUITER DETAILS | ID: %-5d | Name: %-20s | Email: %-25s\n", getId(), getName(), getEmail());
        System.out.printf("                  | Phone: %-15s | Company: %-30s\n", getPhone(), company);
        System.out.println("--------------------------------------------------------------------------------");
    }

    /**
     * Overridden toString method for string representation of Recruiter.
     * @return formatted recruiter string
     */
    @Override
    public String toString() {
        return "Recruiter [ID=" + getId() + ", Name=" + getName() + ", Email=" + getEmail() +
               ", Phone=" + getPhone() + ", Company=" + company + "]";
    }
}
