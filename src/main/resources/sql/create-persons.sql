INSERT INTO persons (
    id, name, coordinates_x, coordinates_y, creation_date,
    height, weight, hair_color, eye_color, nationality,
    location_x, location_y, location_z, location_name
) VALUES
      (1, 'John Smith', 100, 200, '2024-01-15 10:30:00', 180, 75.5, 'BROWN', 'BLUE', 'FRANCE', 10, 20.5, 30.7, 'Paris Office'),
      (2, 'Maria Garcia', -50, 150, '2024-01-20 14:45:00', 165, 62.3, 'ORANGE', 'GREEN', 'SPAIN', 5, 15.2, 25.8, 'Madrid Center'),
      (3, 'Raj Patel', 75, 300, '2024-02-01 09:15:00', 175, 70.0, 'GREEN', 'BROWN', 'INDIA', 0, 0.0, 0.0, 'Unknown Location'),
      (4, 'Somchai Wong', -120, 450, '2024-02-10 16:20:00', NULL, 68.8, 'BLUE', 'ORANGE', 'THAILAND', 30, 40.1, 50.3, 'Bangkok Tower'),
      (5, 'Kim Min-jung', 200, 100, '2024-02-15 11:00:00', 160, 55.2, 'ORANGE', 'BLUE', 'SOUTH_KOREA', 15, 25.6, 35.9, 'Seoul Plaza'),
      (6, 'Pierre Dubois', -80, 250, '2024-03-01 13:30:00', 185, 82.1, 'BROWN', 'GREEN', 'FRANCE', 0, 0.0, 0.0, 'Remote Location'),
      (7, 'Isabella Martinez', 150, 500, '2024-03-05 10:45:00', 170, 58.9, 'GREEN', 'ORANGE', 'SPAIN', 20, 30.4, 40.2, 'Barcelona Beach'),
      (8, 'Priya Sharma', 0, 350, '2024-03-10 15:15:00', 155, 52.4, 'BLUE', 'BROWN', 'INDIA', 8, 18.7, 28.5, 'Mumbai Station'),
      (9, 'Niran Prasert', -200, 626, '2024-03-15 08:30:00', 172, 66.7, 'ORANGE', 'GREEN', 'THAILAND', 0, 0.0, 0.0, 'Home Office'),
      (10, 'Park Ji-ho', 90, 180, '2024-03-20 12:00:00', NULL, 73.5, 'BROWN', 'BLUE', 'SOUTH_KOREA', 12, 22.3, 32.1, 'Busan Port'),
      (11, 'Sophie Laurent', -30, 400, '2024-04-01 14:20:00', 168, 61.0, 'GREEN', 'ORANGE', 'FRANCE', 25, 35.8, 45.6, 'Lyon Square'),
      (12, 'Carlos Rodriguez', 180, 220, '2024-04-05 09:50:00', 178, 77.3, 'BLUE', 'BROWN', 'SPAIN', 0, 0.0, 0.0, 'Work From Home'),
      (13, 'Arun Kumar', -150, 550, '2024-04-10 16:40:00', 182, 79.8, 'ORANGE', 'GREEN', 'INDIA', 18, 28.9, 38.7, 'Delhi Gate'),
      (14, 'Siriporn Chaiyawan', 60, 120, '2024-04-15 11:25:00', 158, 54.6, 'BROWN', 'BLUE', 'THAILAND', 0, 0.0, 0.0, 'Mobile Office'),
      (15, 'Lee Sung-min', -100, 0, '2024-04-20 13:55:00', 176, 69.2, 'GREEN', 'ORANGE', 'SOUTH_KOREA', 22, 32.5, 42.3, 'Incheon Airport');

SELECT setval('persons_id_seq', 15, true);