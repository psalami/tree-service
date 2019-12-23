DELETE FROM children CASCADE;
DELETE FROM nodes CASCADE;

INSERT INTO "public"."nodes"("id","parent","root")
VALUES
(1,NULL,1) ON CONFLICT DO NOTHING;

INSERT INTO "public"."children"("parent","child","depth")
VALUES
(1,1,0) ON CONFLICT DO NOTHING;