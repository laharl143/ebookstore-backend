-- Demo catalogue (spec 0002). No explicit ids and no users: foreign keys use subselects by unique name,
-- so the identity counters stay correct. Prices are VAT exclusive, in PHP.

INSERT INTO categories (name, slug) VALUES
    ('Romance', 'romance'),
    ('Mystery', 'mystery'),
    ('Science Fiction', 'science-fiction'),
    ('Fantasy', 'fantasy'),
    ('Historical', 'historical'),
    ('Biography', 'biography'),
    ('Self-help', 'self-help'),
    ('Memoir', 'memoir'),
    ('Travel', 'travel'),
    ('Cooking', 'cooking'),
    ('Children''s', 'childrens'),
    ('Young Adult', 'young-adult'),
    ('Comics & Graphic Novels', 'comics-graphic-novels'),
    ('Poetry', 'poetry'),
    ('Drama', 'drama'),
    ('Science', 'science'),
    ('Philosophy', 'philosophy'),
    ('Religion', 'religion'),
    ('Language Learning', 'language-learning');

INSERT INTO genres (name) VALUES
    ('Fiction'), ('Non-fiction'), ('Thriller'), ('Horror'), ('Romance'),
    ('Fantasy'), ('Self Help'), ('History'), ('Classic'), ('Filipino Literature');

INSERT INTO authors (name, bio) VALUES
    ('José Rizal', 'Filipino national hero, physician and novelist whose novels exposed abuses under Spanish colonial rule.'),
    ('Jane Austen', 'English novelist known for sharp, witty stories of love and social standing in Regency England.'),
    ('Arthur Conan Doyle', 'Scottish physician and writer, creator of the detective Sherlock Holmes.'),
    ('Mary Shelley', 'English novelist whose Frankenstein is often called the first science fiction novel.'),
    ('H. G. Wells', 'English writer and pioneer of science fiction, author of time travel and alien invasion classics.'),
    ('Bram Stoker', 'Irish author best known for the gothic horror novel Dracula.'),
    ('Maria Santos', 'Manila based writer on everyday habits and Filipino home cooking.'),
    ('Paolo Dizon', 'Travel writer and language teacher who has explored all 81 provinces of the Philippines.'),
    ('Lea Villanueva', 'Award winning author of stories and poems for young Filipino readers.');

INSERT INTO publishers (name, description) VALUES
    ('Lighthouse Classics', 'Affordable editions of the great English language classics.'),
    ('Northwind Editions', 'Mystery, adventure and science fiction, old and new.'),
    ('Mayon Publishing House', 'Filipino literature, history and books for young readers.'),
    ('Pasig River Books', 'Practical non-fiction: self help, cooking, travel and language learning.');

INSERT INTO books (title, description, format, language, price, front_cover_url, back_cover_url, stock_quantity, copies_sold, publish_date, category_id, author_id, publisher_id, created_at, updated_at) VALUES
    ('Noli Me Tángere', 'Rizal''s landmark novel of love and injustice in colonial Philippines.', 'PAPERBACK', 'English', 450.00, 'https://placehold.co/300x450?text=Noli+Me+Tangere', 'https://placehold.co/300x450?text=Back', 30, 120, DATE '2023-06-19',
        (SELECT id FROM categories WHERE slug = 'historical'), (SELECT id FROM authors WHERE name = 'José Rizal'), (SELECT id FROM publishers WHERE name = 'Mayon Publishing House'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Noli Me Tángere', 'Rizal''s landmark novel of love and injustice in colonial Philippines.', 'EBOOK', 'English', 249.00, 'https://placehold.co/300x450?text=Noli+Me+Tangere', 'https://placehold.co/300x450?text=Back', 0, 85, DATE '2023-06-19',
        (SELECT id FROM categories WHERE slug = 'historical'), (SELECT id FROM authors WHERE name = 'José Rizal'), (SELECT id FROM publishers WHERE name = 'Mayon Publishing House'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Noli Me Tángere: Salin sa Filipino', 'Ang klasikong nobela ni Rizal, isinalin sa Filipino.', 'PAPERBACK', 'Filipino', 399.00, 'https://placehold.co/300x450?text=Noli+Salin', 'https://placehold.co/300x450?text=Back', 20, 60, DATE '2024-06-19',
        (SELECT id FROM categories WHERE slug = 'historical'), (SELECT id FROM authors WHERE name = 'José Rizal'), (SELECT id FROM publishers WHERE name = 'Mayon Publishing House'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('El Filibusterismo', 'The darker sequel to Noli Me Tángere, a story of revenge and reform.', 'PAPERBACK', 'English', 450.00, 'https://placehold.co/300x450?text=El+Filibusterismo', 'https://placehold.co/300x450?text=Back', 25, 95, DATE '2023-09-01',
        (SELECT id FROM categories WHERE slug = 'historical'), (SELECT id FROM authors WHERE name = 'José Rizal'), (SELECT id FROM publishers WHERE name = 'Mayon Publishing House'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Pride and Prejudice', 'Elizabeth Bennet and Mr Darcy in Austen''s best loved comedy of manners.', 'PAPERBACK', 'English', 395.00, 'https://placehold.co/300x450?text=Pride+and+Prejudice', 'https://placehold.co/300x450?text=Back', 40, 210, DATE '2022-01-28',
        (SELECT id FROM categories WHERE slug = 'romance'), (SELECT id FROM authors WHERE name = 'Jane Austen'), (SELECT id FROM publishers WHERE name = 'Lighthouse Classics'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Pride and Prejudice', 'A clothbound collector''s edition of Austen''s best loved novel.', 'HARDCOVER', 'English', 1250.00, 'https://placehold.co/300x450?text=Pride+and+Prejudice', 'https://placehold.co/300x450?text=Back', 8, 34, DATE '2024-02-14',
        (SELECT id FROM categories WHERE slug = 'romance'), (SELECT id FROM authors WHERE name = 'Jane Austen'), (SELECT id FROM publishers WHERE name = 'Lighthouse Classics'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Emma', 'A well meaning matchmaker learns that hearts are not hers to arrange.', 'EBOOK', 'English', 199.00, 'https://placehold.co/300x450?text=Emma', 'https://placehold.co/300x450?text=Back', 0, 70, DATE '2022-05-10',
        (SELECT id FROM categories WHERE slug = 'romance'), (SELECT id FROM authors WHERE name = 'Jane Austen'), (SELECT id FROM publishers WHERE name = 'Lighthouse Classics'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Sense and Sensibility', 'Two sisters, one ruled by reason and one by feeling, face love and loss.', 'PAPERBACK', 'English', 375.00, 'https://placehold.co/300x450?text=Sense+and+Sensibility', 'https://placehold.co/300x450?text=Back', 0, 48, DATE '2022-08-15',
        (SELECT id FROM categories WHERE slug = 'romance'), (SELECT id FROM authors WHERE name = 'Jane Austen'), (SELECT id FROM publishers WHERE name = 'Lighthouse Classics'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('The Hound of the Baskervilles', 'Sherlock Holmes investigates a spectral hound on the Devon moors.', 'PAPERBACK', 'English', 350.00, 'https://placehold.co/300x450?text=The+Hound', 'https://placehold.co/300x450?text=Back', 18, 140, DATE '2023-02-03',
        (SELECT id FROM categories WHERE slug = 'mystery'), (SELECT id FROM authors WHERE name = 'Arthur Conan Doyle'), (SELECT id FROM publishers WHERE name = 'Northwind Editions'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('A Study in Scarlet', 'The first meeting of Holmes and Watson, and a murder in Brixton.', 'EBOOK', 'English', 149.00, 'https://placehold.co/300x450?text=A+Study+in+Scarlet', 'https://placehold.co/300x450?text=Back', 0, 66, DATE '2023-04-12',
        (SELECT id FROM categories WHERE slug = 'mystery'), (SELECT id FROM authors WHERE name = 'Arthur Conan Doyle'), (SELECT id FROM publishers WHERE name = 'Northwind Editions'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('The Adventures of Sherlock Holmes', 'Twelve classic cases, in an illustrated hardcover edition.', 'HARDCOVER', 'English', 1450.00, 'https://placehold.co/300x450?text=Adventures+of+Holmes', 'https://placehold.co/300x450?text=Back', 6, 22, DATE '2025-11-20',
        (SELECT id FROM categories WHERE slug = 'mystery'), (SELECT id FROM authors WHERE name = 'Arthur Conan Doyle'), (SELECT id FROM publishers WHERE name = 'Northwind Editions'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Frankenstein', 'A young scientist creates life and must live with what he has made.', 'PAPERBACK', 'English', 375.00, 'https://placehold.co/300x450?text=Frankenstein', 'https://placehold.co/300x450?text=Back', 22, 101, DATE '2022-10-31',
        (SELECT id FROM categories WHERE slug = 'science-fiction'), (SELECT id FROM authors WHERE name = 'Mary Shelley'), (SELECT id FROM publishers WHERE name = 'Lighthouse Classics'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Frankenstein', 'A young scientist creates life and must live with what he has made.', 'EBOOK', 'English', 179.00, 'https://placehold.co/300x450?text=Frankenstein', 'https://placehold.co/300x450?text=Back', 0, 57, DATE '2022-10-31',
        (SELECT id FROM categories WHERE slug = 'science-fiction'), (SELECT id FROM authors WHERE name = 'Mary Shelley'), (SELECT id FROM publishers WHERE name = 'Lighthouse Classics'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('The Time Machine', 'A Victorian inventor travels to the year 802,701 and back.', 'PAPERBACK', 'English', 299.00, 'https://placehold.co/300x450?text=The+Time+Machine', 'https://placehold.co/300x450?text=Back', 35, 88, DATE '2023-07-07',
        (SELECT id FROM categories WHERE slug = 'science-fiction'), (SELECT id FROM authors WHERE name = 'H. G. Wells'), (SELECT id FROM publishers WHERE name = 'Northwind Editions'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('The War of the Worlds', 'Martians land in Surrey, and humanity fights for survival.', 'EBOOK', 'English', 159.00, 'https://placehold.co/300x450?text=War+of+the+Worlds', 'https://placehold.co/300x450?text=Back', 0, 74, DATE '2024-03-18',
        (SELECT id FROM categories WHERE slug = 'science-fiction'), (SELECT id FROM authors WHERE name = 'H. G. Wells'), (SELECT id FROM publishers WHERE name = 'Northwind Editions'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('The Invisible Man', 'A scientist makes himself invisible and cannot undo it.', 'HARDCOVER', 'English', 1100.00, 'https://placehold.co/300x450?text=The+Invisible+Man', 'https://placehold.co/300x450?text=Back', 10, 3, DATE '2026-10-02',
        (SELECT id FROM categories WHERE slug = 'science-fiction'), (SELECT id FROM authors WHERE name = 'H. G. Wells'), (SELECT id FROM publishers WHERE name = 'Northwind Editions'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Dracula', 'Count Dracula leaves Transylvania for England, told in letters and diaries.', 'PAPERBACK', 'English', 425.00, 'https://placehold.co/300x450?text=Dracula', 'https://placehold.co/300x450?text=Back', 15, 92, DATE '2023-10-13',
        (SELECT id FROM categories WHERE slug = 'fantasy'), (SELECT id FROM authors WHERE name = 'Bram Stoker'), (SELECT id FROM publishers WHERE name = 'Lighthouse Classics'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Small Habits, Big Mornings', 'Simple routines that make the first hour of your day count.', 'PAPERBACK', 'English', 520.00, 'https://placehold.co/300x450?text=Small+Habits', 'https://placehold.co/300x450?text=Back', 45, 300, DATE '2025-03-10',
        (SELECT id FROM categories WHERE slug = 'self-help'), (SELECT id FROM authors WHERE name = 'Maria Santos'), (SELECT id FROM publishers WHERE name = 'Pasig River Books'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Small Habits, Big Mornings', 'Simple routines that make the first hour of your day count.', 'EBOOK', 'English', 299.00, 'https://placehold.co/300x450?text=Small+Habits', 'https://placehold.co/300x450?text=Back', 0, 180, DATE '2025-03-10',
        (SELECT id FROM categories WHERE slug = 'self-help'), (SELECT id FROM authors WHERE name = 'Maria Santos'), (SELECT id FROM publishers WHERE name = 'Pasig River Books'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Lutong Bahay: Filipino Home Cooking', 'Mga paboritong lutong bahay, mula adobo hanggang sinigang.', 'HARDCOVER', 'Filipino', 1850.00, 'https://placehold.co/300x450?text=Lutong+Bahay', 'https://placehold.co/300x450?text=Back', 12, 2, DATE '2026-10-05',
        (SELECT id FROM categories WHERE slug = 'cooking'), (SELECT id FROM authors WHERE name = 'Maria Santos'), (SELECT id FROM publishers WHERE name = 'Pasig River Books'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Island Hopping: A Guide to the Visayas', 'Beaches, festivals and food across the islands of the Visayas.', 'PAPERBACK', 'English', 650.00, 'https://placehold.co/300x450?text=Island+Hopping', 'https://placehold.co/300x450?text=Back', 20, 41, DATE '2025-06-01',
        (SELECT id FROM categories WHERE slug = 'travel'), (SELECT id FROM authors WHERE name = 'Paolo Dizon'), (SELECT id FROM publishers WHERE name = 'Pasig River Books'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Tagalog in 30 Days', 'A friendly course in everyday Tagalog for complete beginners.', 'PAPERBACK', 'English', 480.00, 'https://placehold.co/300x450?text=Tagalog+in+30+Days', 'https://placehold.co/300x450?text=Back', 50, 77, DATE '2024-09-09',
        (SELECT id FROM categories WHERE slug = 'language-learning'), (SELECT id FROM authors WHERE name = 'Paolo Dizon'), (SELECT id FROM publishers WHERE name = 'Pasig River Books'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Tagalog in 30 Days', 'A friendly course in everyday Tagalog for complete beginners.', 'EBOOK', 'English', 249.00, 'https://placehold.co/300x450?text=Tagalog+in+30+Days', 'https://placehold.co/300x450?text=Back', 0, 63, DATE '2024-09-09',
        (SELECT id FROM categories WHERE slug = 'language-learning'), (SELECT id FROM authors WHERE name = 'Paolo Dizon'), (SELECT id FROM publishers WHERE name = 'Pasig River Books'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Pepe: A Life of José Rizal', 'A full illustrated biography of the Philippine national hero.', 'HARDCOVER', 'English', 2950.00, 'https://placehold.co/300x450?text=Pepe', 'https://placehold.co/300x450?text=Back', 5, 11, DATE '2025-12-30',
        (SELECT id FROM categories WHERE slug = 'biography'), (SELECT id FROM authors WHERE name = 'Paolo Dizon'), (SELECT id FROM publishers WHERE name = 'Mayon Publishing House'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Ang Munting Kalabaw', 'Isang kuwento tungkol sa maliit na kalabaw na may malaking puso.', 'PAPERBACK', 'Filipino', 199.00, 'https://placehold.co/300x450?text=Munting+Kalabaw', 'https://placehold.co/300x450?text=Back', 30, 5, DATE '2026-10-01',
        (SELECT id FROM categories WHERE slug = 'childrens'), (SELECT id FROM authors WHERE name = 'Lea Villanueva'), (SELECT id FROM publishers WHERE name = 'Mayon Publishing House'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Starlight over Manila Bay', 'Two teenagers, one summer and a promise made at sunset.', 'PAPERBACK', 'English', 420.00, 'https://placehold.co/300x450?text=Starlight', 'https://placehold.co/300x450?text=Back', 25, 39, DATE '2025-02-14',
        (SELECT id FROM categories WHERE slug = 'young-adult'), (SELECT id FROM authors WHERE name = 'Lea Villanueva'), (SELECT id FROM publishers WHERE name = 'Mayon Publishing House'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Mga Tula ng Tag-ulan', 'Mga tula tungkol sa ulan, alaala at pag-ibig.', 'EBOOK', 'Filipino', 99.00, 'https://placehold.co/300x450?text=Tula+ng+Tag-ulan', 'https://placehold.co/300x450?text=Back', 0, 28, DATE '2024-07-15',
        (SELECT id FROM categories WHERE slug = 'poetry'), (SELECT id FROM authors WHERE name = 'Lea Villanueva'), (SELECT id FROM publishers WHERE name = 'Mayon Publishing House'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Genre tags apply to every format of a title.
INSERT INTO book_genres (book_id, genre_id)
SELECT b.id, g.id FROM books b, genres g WHERE
       (b.title IN ('Noli Me Tángere', 'Noli Me Tángere: Salin sa Filipino') AND g.name IN ('Fiction', 'Classic', 'Filipino Literature', 'History'))
    OR (b.title = 'El Filibusterismo' AND g.name IN ('Fiction', 'Classic', 'Filipino Literature'))
    OR (b.title IN ('Pride and Prejudice', 'Emma', 'Sense and Sensibility') AND g.name IN ('Fiction', 'Romance', 'Classic'))
    OR (b.title IN ('The Hound of the Baskervilles', 'A Study in Scarlet', 'The Adventures of Sherlock Holmes') AND g.name IN ('Fiction', 'Thriller', 'Classic'))
    OR (b.title IN ('Frankenstein', 'The Invisible Man') AND g.name IN ('Fiction', 'Horror', 'Classic'))
    OR (b.title = 'The Time Machine' AND g.name IN ('Fiction', 'Classic'))
    OR (b.title = 'The War of the Worlds' AND g.name IN ('Fiction', 'Thriller', 'Classic'))
    OR (b.title = 'Dracula' AND g.name IN ('Fiction', 'Horror', 'Fantasy', 'Classic'))
    OR (b.title = 'Small Habits, Big Mornings' AND g.name IN ('Non-fiction', 'Self Help'))
    OR (b.title = 'Lutong Bahay: Filipino Home Cooking' AND g.name IN ('Non-fiction', 'Filipino Literature'))
    OR (b.title IN ('Island Hopping: A Guide to the Visayas', 'Tagalog in 30 Days') AND g.name = 'Non-fiction')
    OR (b.title = 'Pepe: A Life of José Rizal' AND g.name IN ('Non-fiction', 'History', 'Filipino Literature'))
    OR (b.title = 'Ang Munting Kalabaw' AND g.name IN ('Fiction', 'Filipino Literature'))
    OR (b.title = 'Starlight over Manila Bay' AND g.name IN ('Fiction', 'Romance'))
    OR (b.title = 'Mga Tula ng Tag-ulan' AND g.name = 'Filipino Literature');
