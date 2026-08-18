ALTER TABLE contents
    ADD FULLTEXT INDEX ft_contents_title_content (title, content) WITH PARSER ngram;

ALTER TABLE places
    ADD FULLTEXT INDEX ft_places_name_address (name, address, road_address) WITH PARSER ngram;
