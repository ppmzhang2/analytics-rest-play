# --- Sample dataset

# --- !Ups

INSERT INTO "user" ("id","ntid","name","title") VALUES (
1, 'demome', 'demo user', 'demo user'
);
INSERT INTO "user" ("id","ntid","name","title") VALUES (
2, 'admin', 'administrator', 'administrator'
);
INSERT INTO "user" ("id","ntid","name","title") VALUES (
3, 'guest', 'guest user', 'guest user'
);
INSERT INTO "user" ("id","ntid","name","title") VALUES (
4, 'mzhang2', 'Zhang, Meng', ''
);

INSERT INTO "sql" ("id", "usrid", "desc", "content", "stars", "execution", "exportation", "updated") VALUES (
1, 2, 'sample sql test', 'select date', 0, 0, 0, '2019-11-20');
INSERT INTO "sql" ("id", "usrid", "desc", "content", "stars", "execution", "exportation", "updated") VALUES (
2, 3, 'select all', 'select * from table', 2, 0, 0, '2019-11-19');
INSERT INTO "sql" ("id", "usrid", "desc", "content", "stars", "execution", "exportation", "updated") VALUES (
3, 4, 'select all', 'select * from table', 0, 0, 0, '2019-11-19');
INSERT INTO "sql" ("id", "usrid", "desc", "content", "stars", "execution", "exportation", "updated") VALUES (
4, 4, 'select all', 'select * from table', 0, 0, 0, '2019-11-19');

INSERT INTO "star" ("sqlid", "usrid") VALUES (
2, 2);
INSERT INTO "star" ("sqlid", "usrid") VALUES (
2, 3);

INSERT INTO "comment" ("id", "reid", "usrid", "reusrid", "sqlid", "content", "created") VALUES (
1, 0, 2, 0, 2, 'that is great!', '2019-11-20');
INSERT INTO "comment" ("id", "reid", "usrid", "reusrid", "sqlid", "content", "created") VALUES (
2, 1, 1, 2, 2, 'of course ...', '2019-11-24');
INSERT INTO "comment" ("id", "reid", "usrid", "reusrid", "sqlid", "content", "created") VALUES (
3, 0, 2, 0, 3, 'good example', '2019-11-25');

INSERT INTO "report" ("id", "sqlid", "data", "chart") VALUES (
1,
3,
'{ "filters": [
     { "field": "country", "operator": ">", "operand": "''\"Bonaire''" }
   ],
   "pivotExpr": { "column": "any column", "values": [] },
   "groupExprs": [
     { "fields": ["country", "gender"], "alias": "Any Group Name" }
   ],
   "aggExprs": [
     { "field": "salary", "func": "sum" },
     { "field": "salary", "func": "avg" }
   ],
   "selExprs": [
     { "expr": [{ "field": "`sum(salary)`", "operator": "" }],
       "alias": "`Income Sum`" },
     { "expr": [{ "field": "`avg(salary)`", "operator": "" }],
       "alias": "`Average Income`" }
   ]}',
'{"function": "ColumnChart",
  "title": "Income Comparison - on MO",
  "legend": {
    "position": "bottom",
    "textStyle": {"color": "black"}
  },
  "hAxes": [
    {"title": "X Variable",
     "titleTextStyle": {"color": "black"},
     "gridlines": {"color": "transparent"},
     "showTextEvery": 10,
     "titleTextStyle": {"color": "#333"}
    }
  ],
  "vAxes": [
    {"format":"short",
     "gridlines": {"color": "transparent"},
     "titleTextStyle": {"color": "black"}
    },
    {"format":"percent",
     "gridlines": {"color": "transparent"},
     "viewWindowMode": "explicit",
     "titleTextStyle": {"color": "black"}
    }
  ],
  "series": [
    {"targetAxisIndex": 0,
     "type": "bars",
     "color": "black"
    },
    {"targetAxisIndex": 1,
     "type": "line",
     "color": "red"
    }
  ]
 }');
INSERT INTO "report" ("id", "sqlid", "data", "chart") VALUES (
2,
3,
'{ "filters": [
     { "field": "gender", "operator": "!=", "operand": "''''" },
     { "field": "gender", "operator": "!=", "operand": "''male''" }
   ],
   "pivotExpr": { "column": "any column", "values": [] },
   "groupExprs": [
     {"fields": ["gender"], "alias": "Any Group Name"}
   ],
   "aggExprs": [
     { "field": "salary", "func": "sum" },
     { "field": "salary", "func": "count" }
   ],
   "selExprs": [
     { "expr": [
       { "field": "`sum(salary)`",
         "operator": "" }
       ],
       "alias": "`Total Income`" },
     { "expr": [
       { "field": "`sum(salary)`",
         "operator": "/" },
       { "field": "`count(salary)`",
         "operator": "" }
       ],
       "alias": "`Average Income`" }
   ]}',
'{"function": "Table"}');

# --- !Downs

delete from "report";
delete from "comment";
delete from "star";
delete from "sql";
delete from "user";
