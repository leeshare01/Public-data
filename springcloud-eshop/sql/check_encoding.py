import mysql.connector

try:
    conn = mysql.connector.connect(host='localhost', user='root', password='123456', database='eshop_product')
    cursor = conn.cursor()
    
    print("=== MySQL Character Set Variables ===")
    cursor.execute('SHOW VARIABLES LIKE "character_set%"')
    for row in cursor.fetchall():
        print(f"{row[0]}: {row[1]}")
    
    print("\n=== Collation Variables ===")
    cursor.execute('SHOW VARIABLES LIKE "collation%"')
    for row in cursor.fetchall():
        print(f"{row[0]}: {row[1]}")
    
    print("\n=== Table Collation ===")
    cursor.execute("SHOW TABLE STATUS LIKE 'product'")
    for row in cursor.fetchall():
        print(f"Table: {row[0]}, Collation: {row[4]}")
    
    print("\n=== Sample Data ===")
    cursor.execute("SELECT id, name, main_image FROM product LIMIT 3")
    for row in cursor.fetchall():
        print(f"ID: {row[0]}, Name: {row[1]}, Image: {row[2]}")
    
    cursor.close()
    conn.close()
except Exception as e:
    print(f"Error: {e}")
