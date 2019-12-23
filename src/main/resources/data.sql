DELETE FROM children CASCADE;
DELETE FROM nodes CASCADE;

INSERT INTO "public"."nodes"("id","parent","root")
VALUES
(1,NULL,1),
(2,1,1),
(3,1,1),
(4,2,1),
(5,2,1),
(6,4,1),
(7,4,1),
(8,4,1) ON CONFLICT DO NOTHING;


INSERT INTO "public"."children"("parent","child","depth")
VALUES
(1,1,0),
(1,2,1),
(1,3,1),
(1,4,2),
(1,5,2),
(1,6,3),
(1,7,3),
(1,8,3),
(2,2,0),
(2,4,1),
(2,5,1),
(2,6,2),
(2,7,2),
(2,8,2),
(3,3,0),
(4,4,0),
(4,6,1),
(4,7,1),
(4,8,1),
(5,5,0),
(6,6,0),
(7,7,0),
(8,8,0) ON CONFLICT DO NOTHING;