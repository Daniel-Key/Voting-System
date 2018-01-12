# Voting-System

A secure voting system, implemented using a multi-threaded SSL server. 

Contributors: 
Daniel Key- dgk2@st-andrews.ac.uk

Running instructions: 

> Download all files, and extract if downloaded as a .zip package

> Open two command prompts and navigate in each to the src folder

> Run the command "javac &#42;.java" in one command prompt

> Run the command "java JavaSSLServer ../registeredStudents.csv ../candidates.txt" in the first command prompt

> Run the command "java JavaSSLClient" in the second command prompt

> Refer to the registeredStudents.csv file for authentication information which will be accepted by the server

> The election will end when all registered students have voted, or after one hour

All code is commented and the report provides screenshots and information on design, testing and evaluation.
