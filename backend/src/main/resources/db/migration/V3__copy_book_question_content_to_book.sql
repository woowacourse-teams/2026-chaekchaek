UPDATE book AS b
JOIN book_question AS q ON q.isbn13 = b.isbn13
SET b.question = q.question,
    b.plot = q.plot,
    b.extract = q.extract;
