create function search_app_user(fname character varying, oname character varying, oemail character varying, ophone_number character varying, ostatus character varying, ogender character varying, opage integer, osize integer)
    returns TABLE(total_count bigint, user_data text)
    language plpgsql
as
$$
declare
    dataSql      varchar;
    countSql varchar;
    resultSql varchar;
BEGIN
    dataSql := 'SELECT u.id as userid,' ||
               'u.first_name as firstname,' ||
               'u.last_name as othernames,' ||
               'u.email as email,' ||
               'u.phone_number as phonenumber,' ||
               'u.nationality as nationality,' ||
               'u.gender as gender,' ||
               'u.national_id as identification,' ||
               'u.date_of_birth as dateofbirth,' ||
               'm.active as active' ||
               ' from users u inner join members_tbl m on m.user_id = u.id where u.channel = ' || '''APP'' ';
    countSql := 'SELECT count(u.*) from users u inner join members_tbl m on m.user_id = u.id where u.channel = ' || '''APP'' ';
    if (fname is null or fname = '') then
        dataSql := dataSql || 'and 1 = 1 ';
        countSql := countSql || 'and 1 = 1 ';
    else
        dataSql := dataSql || 'and u.first_name ilike ''%' || fname || '%' || ''' ';
        countSql := countSql || 'and u.first_name ilike ''%' || fname || '%' || ''' ';
    end if;

    if (oname is null or oname = '') then
        dataSql := dataSql || ' and 1 = 1 ';
        countSql := countSql || ' and 1 = 1 ';
    else
        dataSql := dataSql || ' and u.last_name ilike ''%' || oname || '%' || ''' ';
        countSql := countSql || ' and u.last_name ilike ''%' || oname || '%' || ''' ';
    end if;

    if (oemail is null or oemail = '') then
        dataSql := dataSql || ' and 1 = 1 ';
        countSql := countSql || ' and 1 = 1 ';
    else
        dataSql := dataSql || ' and u.email ilike ''%' || oemail || '%' || ''' ';
        countSql := countSql || ' and u.email ilike ''%' || oemail || '%' || ''' ';
    end if;

    if (ostatus is null or ostatus = '') then
        dataSql := dataSql || ' and 1 = 1 ';
        countSql := countSql || ' and 1 = 1 ';
    else
        if (ostatus = 'active') then
            dataSql := dataSql || ' and m.active = true ';
            countSql := countSql || ' and m.active = true ';
        end if;
        if (ostatus = 'inactive') then
            dataSql := dataSql || ' and m.active = false ';
            countSql := countSql || ' and m.active = false ';
        end if;
    end if;

    if (ogender is null or ogender = '') then
        dataSql := dataSql || ' and 1 = 1 ';
        countSql := countSql || ' and 1 = 1 ';
    else
        dataSql := dataSql || ' and u.gender ilike ''' || substr(ogender, 1, 1) || '%' || ''' ';
        countSql := countSql || ' and u.gender ilike ''' || substr(ogender, 1, 1) || '%' || ''' ';
    end if;

    if (ophone_number is null or ophone_number = '') then
        dataSql := dataSql || ' and 1 = 1 ';
        countSql := countSql || ' and 1 = 1 ';
    else
        dataSql := dataSql || ' and u.phone_number ilike ''%' || ophone_number || '%' || '''';
        countSql := countSql || ' and u.phone_number ilike ''%' || ophone_number || '%' || '''';
    end if;

    dataSql := dataSql || ' order by u.created_on desc' || ' limit ' || osize || ' offset ' || '(' || opage || '*' || osize ||
               ')';

    resultSql := 'SELECT (' || countSql || ') as total_count, (SELECT CAST(json_agg(t.*) AS TEXT) FROM (' || dataSql || ') as t' || ') as user_data';

    RETURN QUERY EXECUTE resultSql;
END

$$;

alter function search_app_user(varchar, varchar, varchar, varchar, varchar, varchar, integer, integer) owner to backend;

