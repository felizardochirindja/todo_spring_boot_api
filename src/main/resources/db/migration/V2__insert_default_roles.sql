SET FOREIGN_KEY_CHECKS=0;
truncate table roles;
SET FOREIGN_KEY_CHECKS=1;

insert into roles (name) values
('ADMIN'),
('USER');