package com.amazon.customskill;

import java.sql.Connection;

import java.sql.*;

public class DBSqlite {
	
	
	
	public static Connection  createConnection(){
		
		Connection con = null;
		
		// Loading driver (can be skipped since java 1.5)
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

		
		//create connection
        try {
        	con = DriverManager.getConnection("jdbc:sqlite:C:/sqlite/db/alexa.db");
        } catch (SQLException e) {
            e.printStackTrace();
        }
		
		return con;
	
		
	}
	
	public static void main(String[] args) {

		Connection con = DBSqlite.createConnection();
		delete(con, 1);
		selectTest(con);
		System.out.println(maxIdWords(con));
		System.out.println(maxIdWort(con));
		
    }
	//Tabelle für die englischen Wörter erstellen
    public static void createTableEnglich(Connection con) {
        try (PreparedStatement pstmt = con.prepareStatement("CREATE TABLE IF NOT EXISTS Words (id INTEGER PRIMARY KEY, name string);")) {
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
  //Tabelle für die deutschen Wörter erstellen
    public static void createTableDeutsch(Connection con) {
        try (PreparedStatement pstmt = con.prepareStatement("CREATE TABLE IF NOT EXISTS Wort (id INTEGER PRIMARY KEY, name string);")) {
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
  //Tabelle für alle Wörter erstellen
    public static void createTableAllWords(Connection con) {
        try (PreparedStatement pstmt = con.prepareStatement("CREATE TABLE IF NOT EXISTS AllWords (id INTEGER PRIMARY KEY, name string);")) {
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static boolean insertWords(Connection con, String name) {
        try (PreparedStatement pstmt = con.prepareStatement(
                "INSERT INTO Words (name) VALUES (?);")) {
            pstmt.setString(1,name);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    public static boolean insertWort(Connection con, String name) {
        try (PreparedStatement pstmt = con.prepareStatement(
                "INSERT INTO Wort (name) VALUES (?);")) {
            pstmt.setString(1,name);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    public static boolean insertAllWords(Connection con, String name) {
        try (PreparedStatement pstmt = con.prepareStatement(
                "INSERT INTO AllWords (name) VALUES (?);")) {
            pstmt.setString(1,name);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    

    public static void delete(Connection con, int id) {
        try (PreparedStatement pstmt = con.prepareStatement(
                "DELETE FROM Wort WHERE id = ?;")) {
            pstmt.setInt(1,id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static String selectWords(Connection con,int id) {
    	String name = "";
        try (PreparedStatement pstmt = con.prepareStatement("SELECT * FROM Words where id = "+ id+";");
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
              name = rs.getString("name");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return name;
    }
    
    public static String selectWort(Connection con,int id) {
    	String name = "";
        try (PreparedStatement pstmt = con.prepareStatement("SELECT * FROM Wort where id = "+ id+";");
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
              name = rs.getString("name");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return name;
    }
    
    public static String selectAllWords(Connection con,int id) {
    	String name = "";
        try (PreparedStatement pstmt = con.prepareStatement("SELECT * FROM AllWords where id = "+ id+";");
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
              name = rs.getString("name");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return name;
    }
    
    private static void selectTest(Connection con) {
        try (PreparedStatement pstmt = con.prepareStatement("SELECT * FROM Words;");
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                System.out.println(rs.getInt("id") + ") " + rs.getString("name"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public static int maxIdWords(Connection con) {
    	
    	int count=0;
        try (PreparedStatement pstmt = con.prepareStatement("SELECT MAX(id) as maxid FROM Words;");
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
            	count = rs.getInt("maxid");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return count;
    }
    
    public static int maxIdWort(Connection con) {
    	
    	int count=0;
        try (PreparedStatement pstmt = con.prepareStatement("SELECT MAX(id) as maxid FROM Wort;");
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
            	count = rs.getInt("maxid");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return count;
    }
    
    public static int maxIdAllWords(Connection con) {
    	
    	int count=0;
        try (PreparedStatement pstmt = con.prepareStatement("SELECT MAX(id) as maxid FROM AllWords;");
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
            	count = rs.getInt("maxid");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return count;
    }
    
    public static void drop(Connection con) {
        try (PreparedStatement pstmt = con.prepareStatement("DROP TABLE Wort;")) {
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
}
