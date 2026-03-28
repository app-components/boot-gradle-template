CREATE
  TABLE
    note(
      id bigserial PRIMARY KEY,
      title VARCHAR(255) NOT NULL,
      created_at TIMESTAMP WITH TIME ZONE NOT NULL
    );
