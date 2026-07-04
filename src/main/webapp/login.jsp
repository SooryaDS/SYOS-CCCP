/* ==== Global Styles ==== */
body {
    font-family: "Segoe UI", Arial, sans-serif;
    background-color: #f8f9fa;
    margin: 0;
    padding: 0;
    display: flex;
    justify-content: center;
    align-items: center;
    height: 100vh;
}

/* ==== Login Container ==== */
form {
    background: #ffffff;
    padding: 2.5rem;
    border-radius: 12px;
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
    text-align: center;
    width: 320px;
}

h2 {
    margin-bottom: 20px;
    color: #333333;
    font-weight: 600;
}

/* ==== Input Fields ==== */
input[type="text"],
input[type="password"] {
    width: 90%;
    padding: 12px;
    margin: 10px 0;
    border: 1px solid #ccc;
    border-radius: 8px;
    font-size: 15px;
    transition: border-color 0.3s ease;
}

input[type="text"]:focus,
input[type="password"]:focus {
    border-color: #007BFF;
    outline: none;
    box-shadow: 0 0 5px rgba(0, 123, 255, 0.2);
}

/* ==== Buttons ==== */
button {
    width: 95%;
    padding: 12px;
    background-color: #007BFF;
    color: white;
    font-size: 16px;
    font-weight: 600;
    border: none;
    border-radius: 8px;
    cursor: pointer;
    margin-top: 15px;
    transition: background-color 0.3s ease;
}

button:hover {
    background-color: #0056b3;
}

/* ==== Links ==== */
a {
    display: inline-block;
    margin-top: 10px;
    color: #007BFF;
    text-decoration: none;
    font-size: 14px;
    transition: color 0.3s ease;
}

a:hover {
    color: #0056b3;
    text-decoration: underline;
}

/* ==== Error Message ==== */
p[style*="color:red"] {
    background: #ffe6e6;
    color: #cc0000 !important;
    padding: 10px;
    border-radius: 8px;
    font-size: 14px;
    margin-bottom: 15px;
}
