# payroll-system
A desktop-based payroll management application built using Java Swing and MySQL, designed to handle employee details, salary computation, and payslip generation. The system provides a complete CRUD workflow with real-time salary calculation and PDF export functionality.

🔧 Tech Stack

Java (Swing) – GUI development,
MySQL – Database & relational data storage,
JDBC – Database connectivity,
iText – Automated PDF payslip generation.

✨ Features

Add, update, and delete employee records

Automatic table creation for employee and salary (with foreign key constraints)

Salary calculation based on Basic Pay, HRA, Allowances & Deductions

Displays employee list using JTable

Generates formatted payslips inside the app

Exports payslips as A4 PDF files with month/year stamping

Data validation, transaction handling & error prompts

Clean and responsive user interface

📦 Functional Modules

Employee Management

Salary Management

Payslip Viewer

PDF Export (iText)

Database Initialization & Auto Schema Creation

📁 Project Structure

Java Swing GUI (JFrame, JTable, buttons, input fields)

MySQL backend with two linked tables

Utility functions for form handling, table refresh, and input parsing
