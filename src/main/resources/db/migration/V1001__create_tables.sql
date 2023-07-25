CREATE TABLE abac_demo.employee
(
    id       UUID    NOT NULL PRIMARY KEY DEFAULT uuid_generate_v4(),
    name     TEXT,
    branch   TEXT,
    roles    JSONB   NOT NULL             DEFAULT '[]',
    skills   JSONB   NOT NULL             DEFAULT '[]',
    passport TEXT,
    salary   INTEGER NOT NULL             DEFAULT 0
);

COMMENT ON TABLE abac_demo.employee IS 'Logos Media Holding Employees';
COMMENT ON COLUMN abac_demo.employee.id IS 'Unique ID';
COMMENT ON COLUMN abac_demo.employee.name IS 'Name';
COMMENT ON COLUMN abac_demo.employee.branch IS 'Holdings branch';
COMMENT ON COLUMN abac_demo.employee.roles IS 'Roles';
COMMENT ON COLUMN abac_demo.employee.skills IS 'List of competencies';
COMMENT ON COLUMN abac_demo.employee.passport IS 'Passport number';
COMMENT ON COLUMN abac_demo.employee.salary IS 'Salary in USD';

CREATE TABLE abac_demo.publication
(
    id               UUID NOT NULL PRIMARY KEY DEFAULT uuid_generate_v4(),
    branch           TEXT,
    theme            TEXT,
    author_id        UUID NOT NULL REFERENCES abac_demo.employee (id),
    status           TEXT NOT NULL             DEFAULT 'In progress',
    publication_date DATE,
    title            TEXT
);

COMMENT ON TABLE abac_demo.publication IS 'Publications of Logos Media Holding';
COMMENT ON COLUMN abac_demo.publication.id IS 'Unique ID';
COMMENT ON COLUMN abac_demo.publication.branch IS 'Holdings branch';
COMMENT ON COLUMN abac_demo.publication.theme IS 'Publication theme';
COMMENT ON COLUMN abac_demo.publication.author_id IS 'Authors ID';
COMMENT ON COLUMN abac_demo.publication.status IS 'Publication status';
COMMENT ON COLUMN abac_demo.publication.publication_date IS 'Publication display date on the information portal';
COMMENT ON COLUMN abac_demo.publication.title IS 'Publication title';