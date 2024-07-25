# --- !Ups

CREATE TABLE items (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    price NUMERIC(10, 2) NOT NULL,
    description VARCHAR(255) NOT NULL
);

INSERT INTO items (name, price, description) VALUES ('Makers T-shirt', 10.00, 'A lovely T-shirt from Makers')

# --- !Downs

DROP TABLE items;
