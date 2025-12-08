package org.bhel.hrm.server.domain;

public class Employee {
    private int id;
    private int userId;
    private String firstName;
    private String lastName;
    private String icPassport;

    // You could add complex, non-serializable fields here in the future
    // private PerformanceReview lastReview;

    public Employee() {}

    public Employee(int id, String firstName, String lastName, String icPassport) {
        this.userId = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.icPassport = icPassport;
    }
    public Employee(int id, int userId, String firstName, String lastName, String icPassport) {
        this.id = id;
        this.userId = userId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.icPassport = icPassport;
    }

    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }
    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getIcPassport() {
        return icPassport;
    }

    public void setIcPassport(String icPassport) {
        this.icPassport = icPassport;
    }
}
