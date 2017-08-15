Warning: The Primary Relationships table should be expandable for every new feature we introduce. 
For that reason, materialized views should not be added to that table since the presence of MVs
can prevent the table from being altered.
