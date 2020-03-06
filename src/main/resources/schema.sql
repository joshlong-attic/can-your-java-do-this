create table PEOPLE
(
    id              serial primary key,
    name            varchar(255) not null,
    emotional_state int
);