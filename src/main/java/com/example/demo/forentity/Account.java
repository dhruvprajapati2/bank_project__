package com.example.demo.forentity;

import jakarta.persistence.*;

@Entity
@Table(
    name = "accounts",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = "account_no")
    }
)
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_no", nullable = false, unique = true, length = 20)
    private Long accountNo;

    @Column(name = "user_name", nullable = false, length = 100)
    private String userName;

    @Column(name = "balance", nullable = false)
    private double balance;

      // ---------- Optional (Debugging) ----------

    public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getAccountNo() {
		return accountNo;
	}

	public void setAccountNo(Long accountNo) {
		this.accountNo = accountNo;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public double getBalance() {
		return balance;
	}

	public void setBalance(double balance) {
		this.balance = balance;
	}

	@Override
    public String toString() {
        return "Account{" +
                "id=" + id +
                ", accountNo='" + accountNo + '\'' +
                ", userName='" + userName + '\'' +
                ", balance=" + balance +
                '}';
    }

	public String getEmail() {
		// TODO Auto-generated method stub
		return null;
	}
	
	
}