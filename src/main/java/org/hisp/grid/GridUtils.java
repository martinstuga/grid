package org.hisp.grid;

import java.io.IOException;
import java.io.Writer;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hisp.grid.csv.CsvWriteOptions;
import org.hisp.grid.util.MapBuilder;

import com.csvreader.CsvWriter;

/**
 * Utility methods for {@link Grid}.
 */
public class GridUtils
{
    private static final String EMPTY = "";

    private static final Map<Integer, ValueType> SQL_VALUE_TYPE_MAP = new MapBuilder<Integer, ValueType>()
        .put( Types.BIT, ValueType.BOOLEAN )
        .put( Types.TINYINT, ValueType.SMALLINT )
        .put( Types.SMALLINT, ValueType.SMALLINT )
        .put( Types.INTEGER, ValueType.INTEGER )
        .put( Types.BIGINT, ValueType.BIGINT )
        .put( Types.FLOAT, ValueType.DOUBLE )
        .put( Types.REAL, ValueType.REAL )
        .put( Types.DOUBLE, ValueType.DOUBLE )
        .put( Types.NUMERIC, ValueType.NUMERIC )
        .put( Types.DECIMAL, ValueType.DOUBLE )
        .put( Types.CHAR, ValueType.CHAR )
        .put( Types.VARCHAR, ValueType.TEXT )
        .put( Types.LONGVARCHAR, ValueType.TEXT )
        .put( Types.DATE, ValueType.DATE )
        .put( Types.TIME, ValueType.TIMESTAMP )
        .put( Types.TIMESTAMP, ValueType.TIMESTAMP )
        .put( Types.BINARY, ValueType.BOOLEAN )
        .put( Types.VARBINARY, ValueType.BOOLEAN )
        .put( Types.LONGVARBINARY, ValueType.BOOLEAN )
        .put( Types.BOOLEAN, ValueType.BOOLEAN )
        .put( Types.TIME_WITH_TIMEZONE, ValueType.TIMESTAMPTZ )
        .put( Types.TIMESTAMP_WITH_TIMEZONE, ValueType.TIMESTAMPTZ )
        .build();

    /**
     * Creates a {@link Grid} based on the given SQL {@link ResultSet}.
     *
     * @param rs the {@link ResultSet}.
     * @return a {@link Grid}.
     */
    public static Grid fromResultSet( ResultSet rs )
    {
        Grid grid = new ListGrid();
        addHeaders( grid, rs );
        addRows( grid, rs );
        return grid;
    }

    /**
     * Renders the given {@link Grid} in CSV format. Writes the
     * content to the given {@link Writer}.
     *
     * @param grid the grid.
     * @param writer the writer.
     * @throws IOException for errors during rendering.
     */
    public static void toCsv( Grid grid, Writer writer )
        throws IOException
    {
        toCsv( grid, writer, new CsvWriteOptions() );
    }

    /**
     * Renders the given {@link Grid} in CSV format. Writes the
     * content to the given {@link Writer}.
     *
     * @param grid the grid.
     * @param writer the writer.
     * @param options the {@link CsvWriteOptions}.
     * @throws IOException for errors during rendering.
     */
    public static void toCsv( Grid grid, Writer writer, CsvWriteOptions options )
        throws IOException
    {
        if ( grid == null )
        {
            return;
        }

        CsvWriter csvWriter = getCsvWriter( writer, options );

        Iterator<GridHeader> headers = grid.getHeaders().iterator();

        if ( !grid.getHeaders().isEmpty() )
        {
            while ( headers.hasNext() )
            {
                csvWriter.write( headers.next().getName() );
            }

            csvWriter.endRecord();
        }

        for ( List<Object> row : grid.getRows() )
        {
            Iterator<Object> columns = row.iterator();

            while ( columns.hasNext() )
            {
                Object value = columns.next();

                csvWriter.write( value != null ? String.valueOf( value ) : EMPTY );
            }

            csvWriter.endRecord();
        }

        csvWriter.flush();
    }

    /**
     * Returns a list based on the given variable arguments.
     *
     * @param items the items.
     * @param <T> type.
     * @return a list of the given items.
     */
    @SafeVarargs
    public static <T> List<T> getList( T... items )
    {
        List<T> list = new ArrayList<>();
        Collections.addAll( list, items );
        return list;
    }

    // ---------------------------------------------------------------------
    // Supportive methods
    // ---------------------------------------------------------------------

    private static void addHeaders( Grid grid, ResultSet rs )
    {
        try
        {
            ResultSetMetaData rsmd = rs.getMetaData();

            int columnNo = rsmd.getColumnCount();

            for ( int i = 1; i <= columnNo; i++ )
            {
                String label = rsmd.getColumnLabel( i );
                String name = rsmd.getColumnName( i );
                ValueType valueType = fromSqlType( rsmd.getColumnType( i ) );

                grid.addHeader( new GridHeader( label, name, valueType, false, false ) );
            }
        }
        catch ( SQLException ex )
        {
            throw new RuntimeException( ex );
        }
    }

    private static void addRows( Grid grid, ResultSet rs )
    {
        try
        {
            int cols = rs.getMetaData().getColumnCount();

            while ( rs.next() )
            {
                grid.addRow();

                for ( int i = 1; i <= cols; i++ )
                {
                    grid.addValue( rs.getObject( i ) );
                }
            }
        }
        catch ( SQLException ex )
        {
            throw new RuntimeException( ex );
        }
    }

    private static ValueType fromSqlType( Integer sqlType )
    {
        return SQL_VALUE_TYPE_MAP.getOrDefault( sqlType, ValueType.TEXT );
    }

    private static CsvWriter getCsvWriter( Writer writer, CsvWriteOptions options )
    {
        CsvWriter csvWriter = new CsvWriter( writer, options.getDelimiter() );
        csvWriter.setForceQualifier( options.isForceQualifier() );
        return csvWriter;
    }
}
