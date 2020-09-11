package cz.iwitrag.procdungen.api.data;

import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import cz.iwitrag.procdungen.api.data.files.*;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Represents entire dungeon as generated by generator<br>
 * Every dungeons consists of {@link Room rooms} and {@link Corridor corridors} and can output its data as {@link Grid} as well
 */
public class Dungeon {

    /** Generated rooms */
    private Set<Room> rooms = new LinkedHashSet<>();
    /** Generated corridors */
    private Set<Corridor> corridors = new LinkedHashSet<>();
    /** Dungeon as a grid of cells */
    private Grid grid = null;
    /** Used seed */
    private long seed = -1;

    /**
     * Returns entire dungeon divided to grid made of cells<br>
     * Every cell may be empty space, room or corridor<br>
     * After first call grid gets cached until structure of dungeon is changed<br>
     * When that happens grid is lost and must be recalculated
     * @return Grid made cells
     */
    public Grid getGrid() {
        if (grid != null)
            return grid;

        grid = new Grid();

        for (Room room : rooms) {
            int roomX = room.getPosition().getX();
            int roomY = room.getPosition().getY();
            for (GridCell cell : room.getShape()) {
                if (cell.getType() == GridCell.Type.NONE)
                    continue;

                int x = roomX + cell.getPosition().getX();
                int y = roomY + cell.getPosition().getY();

                GridCell newCell = new GridCell(x, y, cell.getType());
                newCell.setRoom(room);
                grid.setGridCell(x, y, newCell);
            }
        }

        for (Corridor corridor : corridors) {
            int corridorX = corridor.getPosition().getX();
            int corridorY = corridor.getPosition().getY();
            for (GridCell cell : corridor.getShape()) {
                if (cell.getType() == GridCell.Type.NONE)
                    continue;

                int x = corridorX + cell.getPosition().getX();
                int y = corridorY + cell.getPosition().getY();

                GridCell newCell = new GridCell(x, y, cell.getType());
                newCell.setCorridor(corridor);
                grid.setGridCell(x, y, newCell);
            }
        }

        return grid;
    }

    public Set<Room> getRooms() {
        return new LinkedHashSet<>(rooms);
    }

    public void setRooms(Collection<Room> rooms) {
        this.rooms.clear();
        this.rooms.addAll(rooms);
        this.grid = null;
    }

    public void addRoom(Room room) {
        rooms.add(room);
        this.grid = null;
    }

    public void removeRoom(Room room) {
        rooms.remove(room);
        this.grid = null;
    }

    /**
     * Returns room at given position
     * @param x X coordinate
     * @param y Y coordinate
     * @param exactShape Whether room shapes should be checked exactly (otherwise shapes complemented to rectangles will be used)
     * @return Room at given position or null if none found
     */
    public Room getRoomAtPosition(int x, int y, boolean exactShape) {
        DungeonRectangle dungeonRectangle = getDungeonRectangleAtPosition(x, y, true, false, exactShape);
        return dungeonRectangle == null ? null : (Room)dungeonRectangle;
    }

    /**
     * Returns corridor at given position
     * @param x X coordinate
     * @param y Y coordinate
     * @param exactShape Whether corridor shapes should be checked exactly (otherwise shapes complemented to rectangles will be used)
     * @return Corridor at given position or null if none found
     */
    public Corridor getCorridorAtPosition(int x, int y, boolean exactShape) {
        DungeonRectangle dungeonRectangle = getDungeonRectangleAtPosition(x, y, false, true, exactShape);
        return dungeonRectangle == null ? null : (Corridor)dungeonRectangle;
    }

    private DungeonRectangle getDungeonRectangleAtPosition(int x, int y, boolean rooms, boolean corridors, boolean exactShape) {
        DungeonRectangle result = null;
        Set<DungeonRectangle> search = new LinkedHashSet<>();
        if (rooms)
            search.addAll(this.rooms);
        if (corridors)
            search.addAll(this.corridors);
        for (DungeonRectangle dungeonRectangle : search) {
            if (dungeonRectangle.getTopLeftCorner().getX() <= x
                    && dungeonRectangle.getTopRightCorner().getX() > x
                    && dungeonRectangle.getTopLeftCorner().getY() <= y
                    && dungeonRectangle.getBottomLeftCorner().getY() > y) {
                result = dungeonRectangle;
                break;
            }
        }
        if (result == null)
            return null;
        if (exactShape) {
            int shapeX = x - result.getTopLeftCorner().getX();
            int shapeY = y - result.getTopLeftCorner().getY();
            if (result.getShape().getGridCell(shapeX, shapeY).getType() == GridCell.Type.NONE)
                return null;
            else
                return result;
        }
        else
            return result;
    }

    public Set<Corridor> getCorridors() {
        return new LinkedHashSet<>(corridors);
    }

    public void setCorridors(Collection<Corridor> corridors) {
        this.corridors.clear();
        this.corridors.addAll(corridors);
        this.grid = null;
    }

    public void addCorridor(Corridor corridor) {
        corridors.add(corridor);
        this.grid = null;
    }

    public void removeCorridor(Corridor corridor) {
        corridors.remove(corridor);
        this.grid = null;
    }
    
    public long getSeed() {
        return seed;
    }

    public void setSeed(long seed) {
        this.seed = seed;
    }

    /**
     * Saves dungeon data to YAML
     * @param file File to save to
     * @throws IOException When I/O error occurs
     */
    public void toYaml(File file) throws IOException {
        YAMLFactory yamlFactory = new YAMLFactory();
        yamlFactory.disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER);
        toFile(file, new ObjectMapper(yamlFactory), null);
    }

    /**
     * Saves dungeon data to JSON
     * @param file File to save to
     * @throws IOException When I/O error occurs
     */
    public void toJson(File file) throws IOException {
        DefaultPrettyPrinter prettyPrinter = new DefaultPrettyPrinter();
        prettyPrinter.indentArraysWith(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE);
        toFile(file, new ObjectMapper(), prettyPrinter);
    }

    /**
     * Saves dungeon data to XML
     * @param file File to save to
     * @throws IOException When I/O error occurs
     */
    public void toXml(File file) throws IOException {
        toFile(file, new XmlMapper(), null);
    }

    private void toFile(File file, ObjectMapper mapper, DefaultPrettyPrinter prettyPrinter) throws IOException {
        Objects.requireNonNull(file, "Cannot write Dungeon to null file");
        Objects.requireNonNull(mapper, "Cannot write Dungeon with null mapper");
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        if (prettyPrinter == null)
            mapper.writeValue(file, toPojo());
        else
            mapper.writer(prettyPrinter).writeValue(file, toPojo());
    }

    private DungeonPojo toPojo() {
        DungeonPojo dungeonPojo = new DungeonPojo();
        dungeonPojo.setSeed(this.seed);

        // Rooms
        List<RoomPojo> roomPojoList = new ArrayList<>();
        for (Room room : rooms) {
            RoomPojo roomPojo = new RoomPojo();
            roomPojo.setId(room.getId());
            roomPojo.setName(room.getName());
            roomPojo.setShape(room.getShape().getTextualShape().getShapeRows());
            roomPojo.setShapeName(room.getShapeName());
            Point<Integer> roomPosition = room.getPosition();
            roomPojo.setPosition(new PositionPojo(roomPosition.getX(), roomPosition.getY()));
            List<Integer> connectedRooms = new ArrayList<>();
            for (Room connectedRoom : room.getConnectedRooms()) {
                connectedRooms.add(connectedRoom.getId());
            }
            roomPojo.setConnections(connectedRooms);
            roomPojo.setRotated(room.getShape().getTextualShape().getRotated());
            roomPojoList.add(roomPojo);
        }
        dungeonPojo.setRooms(roomPojoList);

        // Corridors
        List<CorridorPojo> corridorPojoList = new ArrayList<>();
        for (Corridor corridor : corridors) {
            CorridorPojo corridorPojo = new CorridorPojo();
            corridorPojo.setId(corridor.getId());
            corridorPojo.setShape(corridor.getShape().getTextualShape().getShapeRows());
            Point<Integer> corridorPosition = corridor.getPosition();
            corridorPojo.setPosition(new PositionPojo(corridorPosition.getX(), corridorPosition.getY()));
            List<Integer> connectedRooms = new ArrayList<>();
            for (Room connectedRoom : corridor.getConnectedRooms()) {
                connectedRooms.add(connectedRoom.getId());
            }
            corridorPojo.setConnections(connectedRooms);
            corridorPojoList.add(corridorPojo);
        }
        dungeonPojo.setCorridors(corridorPojoList);

        // Grid
        Grid grid = getGrid();
        GridPojo gridPojo = new GridPojo();
        gridPojo.setWidth(grid.getWidth());
        gridPojo.setHeight(grid.getHeight());
        List<GridCellPojo> gridCellPojoList = new ArrayList<>();
        for (GridCell cell : grid) {
            GridCellPojo gridCellPojo = new GridCellPojo();
            Point<Integer> cellPosition = cell.getPosition();
            gridCellPojo.setPosition(new PositionPojo(cellPosition.getX(), cellPosition.getY()));
            gridCellPojo.setType(cell.getType());
            if (cell.getRoom() != null)
                gridCellPojo.setRoom(cell.getRoom().getId());
            if (cell.getCorridor() != null)
                gridCellPojo.setCorridor(cell.getCorridor().getId());
            gridCellPojoList.add(gridCellPojo);
        }
        gridPojo.setCells(gridCellPojoList);
        dungeonPojo.setGrid(gridPojo);

        return dungeonPojo;
    }

    @Override
    public String toString() {
        return "Dungeon with " + rooms.size() + " rooms and " + corridors.size() + " corridors, seed " + seed;
    }
}