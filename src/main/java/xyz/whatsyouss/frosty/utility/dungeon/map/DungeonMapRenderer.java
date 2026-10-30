package xyz.whatsyouss.frosty.utility.dungeon.map;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import org.joml.Matrix3x2fStack;
import xyz.whatsyouss.frosty.utility.HotbarSwapUtils;
import xyz.whatsyouss.frosty.utility.dungeon.map.DungeonMapOptions.MapColor;
import xyz.whatsyouss.frosty.utility.dungeon.map.DungeonMapOptions.RoomInfoAlignment;
import xyz.whatsyouss.frosty.utility.dungeon.map.DungeonMapOptions.TextShadow;
import xyz.whatsyouss.frosty.utility.dungeon.map.DungeonPositions.Component;
import xyz.whatsyouss.frosty.utility.dungeon.map.DungeonPositions.PlayerComponent;
import xyz.whatsyouss.frosty.utility.dungeon.map.DungeonPositions.WorldComponent;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static xyz.whatsyouss.frosty.Frosty.mc;

public final class DungeonMapRenderer {

    private static final int MARKER_ATLAS_WIDTH = 200;
    private static final int MARKER_ATLAS_HEIGHT = 280;

    private static final float[] MARKER_SELF = {0, 0, 100, 140};
    private static final float[] MARKER_OTHER = {100, 0, 100, 140};
    private static final float[] MARKER_POINTER_OUTLINE = {0, 140, 100, 140};
    private static final float[] MARKER_HEAD_OUTLINE = {100, 140, 80, 80};

    private static final String[] ICON_STYLES = {"illegalmap", "neu", "vanillanew", "vanillaold"};

    private static final Map<String, String> SPECIAL_ROOMS = Map.of(
            "Creeper Beams", "creeper",
            "Three Weirdos", "chest",
            "Tic Tac Toe", "shears",
            "Water Board", "bucket_water",
            "Teleport Maze", "endframe_side",
            "Blaze", "blaze_powder",
            "Boulder", "planks_oak",
            "Ice Fill", "ice",
            "Ice Path", "spawner",
            "Quiz", "book_normal");

    private static final Identifier MARKER_ATLAS =
            Identifier.fromNamespaceAndPath("frosty", "dungeons/map/marker_atlas.png");

    private DungeonMapRenderer() {
    }

    public static void draw(GuiGraphicsExtractor ctx, Font font, float x, float y, float size,
                            DungeonMapOptions options, List<DungeonMapRoomData> rooms, DungeonDoor[] doors,
                            float hudScale) {
        FloorType floor = DungeonTracker.floor == FloorType.None ? FloorType.M7 : DungeonTracker.floor;

        options.dungeonWidth = floor.roomsW;
        options.dungeonHeight = floor.roomsH;

        float maxDim = Math.max(options.dungeonWidth, options.dungeonHeight);
        float totalMaxDim = (float) (maxDim + options.padding * 2);
        float compToPx = size / totalMaxDim;
        float ox = (float) ((maxDim - options.dungeonWidth) / 2.0 + options.padding) * compToPx;
        float oy = (float) ((maxDim - options.dungeonHeight) / 2.0 + options.padding) * compToPx;

        Matrix3x2fStack pose = ctx.pose();
        pose.pushMatrix();
        pose.translate(x, y);

        int background = DungeonMapOptions.argb(options.color(MapColor.Background));
        if ((background >>> 24) != 0) {
            ctx.fill(0, 0, Math.round(size), Math.round(size), background);
        }

        drawBorder(ctx, size, options);

        boolean scissored = false;
        if (options.rotate) {
            PlayerComponent self = DungeonTracker.selfPlayer == null ? null : DungeonTracker.selfPlayer.getLerpedPosition();
            if (self != null) {
                float px = mapX((float) self.x(), maxDim, ox, compToPx);
                float py = mapY((float) self.z(), maxDim, oy, compToPx);
                pose.translate(size * 0.5f, size * 0.5f);
                pose.rotate((float) (self.r() - Math.PI * 0.5));
                pose.translate(-px, -py);

                ctx.enableScissor(Math.round(x), Math.round(y), Math.round(x + size), Math.round(y + size));
                scissored = true;
            }
        }

        drawDoors(ctx, options, doors, ox, oy, compToPx);
        drawRooms(ctx, font, options, rooms, ox, oy, compToPx);

        if (!DungeonTracker.inBoss || options.showInBoss) {
            drawPlayers(ctx, font, options, floor, maxDim, ox, oy, compToPx, hudScale);
        }

        if (scissored) ctx.disableScissor();
        pose.popMatrix();
    }

    private static float mapX(float componentX, float maxDim, float ox, float compToPx) {
        return (float) MapMath.rescale(componentX, 0, maxDim * 2.0, ox, ox + maxDim * compToPx);
    }

    private static float mapY(float componentZ, float maxDim, float oy, float compToPx) {
        return (float) MapMath.rescale(componentZ, 0, maxDim * 2.0, oy, oy + maxDim * compToPx);
    }

    private static int px(float value) {
        return Math.round(value);
    }

    private static void drawBorder(GuiGraphicsExtractor ctx, float size, DungeonMapOptions options) {
        int borderColor = DungeonMapOptions.argb(options.color(MapColor.Border));
        if (options.border <= 0 || (borderColor >>> 24) == 0) return;

        int w = Math.round(size);
        int lw = options.border;

        ctx.fill(0, 0, w, lw, borderColor);
        ctx.fill(0, lw, lw, w, borderColor);
        ctx.fill(w - lw, lw, w, w, borderColor);
        ctx.fill(lw, w - lw, w - lw, w, borderColor);
    }

    private static int baseColorForRoom(DungeonMapOptions options, RoomType type, ClearType clear) {
        Color color = switch (type) {
            case ENTRANCE -> options.color(MapColor.RoomEntrance);
            case NORMAL -> clear == ClearType.MINIBOSS
                    ? options.color(MapColor.RoomMiniboss) : options.color(MapColor.RoomNormal);
            case FAIRY -> options.color(MapColor.RoomFairy);
            case BLOOD -> options.color(MapColor.RoomBlood);
            case PUZZLE -> options.color(MapColor.RoomPuzzle);
            case TRAP -> options.color(MapColor.RoomTrap);
            case YELLOW -> options.color(MapColor.RoomYellow);
            case RARE -> options.color(MapColor.RoomRare);
            case UNKNOWN -> options.color(MapColor.RoomUnknown);
        };
        return DungeonMapOptions.argb(color);
    }

    private static int darkenRoomColor(DungeonMapOptions options, int argb, boolean explored) {
        if ((argb >>> 24) == 0 || !options.dungeonStarted || explored) return argb;

        double factor = options.unknownRoomsDarkenFactor;
        int a = (argb >>> 24) & 0xFF;
        int r = (int) (((argb >> 16) & 0xFF) * factor + 0.5);
        int g = (int) (((argb >> 8) & 0xFF) * factor + 0.5);
        int b = (int) ((argb & 0xFF) * factor + 0.5);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int colorForRoom(DungeonMapOptions options, DungeonRoom room) {
        int argb;
        if (!room.explored && (!options.renderUnknownRooms || room.name == null)) {
            argb = DungeonMapOptions.argb(options.color(MapColor.RoomUnknown));
        } else {
            argb = baseColorForRoom(options, room.type, room.clear);
        }
        return darkenRoomColor(options, argb, room.explored);
    }

    private static int predictionColor(DungeonMapOptions options, RoomType type, DungeonRoom room) {
        return darkenRoomColor(options, baseColorForRoom(options, type, null), room.explored);
    }

    private static void drawDoors(GuiGraphicsExtractor ctx, DungeonMapOptions options, DungeonDoor[] doors,
                                  float ox, float oy, float compToPx) {
        float roomRectOffset = (float) ((1.0 - options.roomWidth) * 0.5);
        float doorOffset = (float) ((1.0 - options.doorWidth) * 0.5);

        for (DungeonDoor door : doors) {
            if (door == null) continue;
            if (!options.renderUnknownRooms && door.rooms.stream().noneMatch(room -> room.explored)) continue;

            int color;
            switch (door.type) {
                case ENTRANCE -> color = DungeonMapOptions.argb(options.color(MapColor.DoorEntrance));
                case WITHER -> color = DungeonMapOptions.argb(options.color(MapColor.DoorWither));
                case BLOOD -> color = DungeonMapOptions.argb(options.color(MapColor.DoorBlood));
                default -> {
                    if (!door.opened) continue;

                    Color fixed = options.color(MapColor.DoorNormal);
                    if (fixed != null) {
                        color = DungeonMapOptions.argb(fixed);
                    } else {
                        DungeonRoom room = door.rooms.stream()
                                .min((a, b) -> Integer.compare(
                                        a.type.prio - (!a.explored && !options.renderUnknownRooms ? 100 : 0),
                                        b.type.prio - (!b.explored && !options.renderUnknownRooms ? 100 : 0)))
                                .orElse(null);
                        if (room == null) continue;

                        if (!room.explored && door.fairyDoorFlashing) {
                            color = DungeonMapOptions.argb(options.color(MapColor.DoorWither));
                        } else {
                            color = colorForRoom(options, room);
                        }
                    }
                }
            }

            if ((color >>> 24) == 0) {
                color = DungeonMapOptions.argb(options.color(MapColor.RoomNormal));
                if ((color >>> 24) == 0) continue;
            }

            boolean horizontal = door.roomComp1.x() == door.roomComp2.x();
            int x1 = door.roomComp1.x();
            int z1 = door.roomComp1.z();
            int x2 = door.roomComp2.x();
            int z2 = door.roomComp2.z();
            if (x1 > x2 || z1 > z2) {
                int tx = x1;
                int tz = z1;
                x1 = x2;
                z1 = z2;
                x2 = tx;
                z2 = tz;
            }

            float fx;
            float fy;
            float fw;
            float fh;
            if (horizontal) {
                fx = x1 + doorOffset;
                fy = z1 + roomRectOffset + (float) options.roomWidth;
                fw = (float) options.doorWidth;
                fh = roomRectOffset * 2f;
            } else {
                fx = x1 + roomRectOffset + (float) options.roomWidth;
                fy = z1 + doorOffset;
                fw = roomRectOffset * 2f;
                fh = (float) options.doorWidth;
            }

            int bx = px(ox + fx * compToPx);
            int by = px(oy + fy * compToPx);
            int bw = (int) Math.ceil(fw * compToPx);
            int bh = (int) Math.ceil(fh * compToPx);
            ctx.fill(bx, by, bx + bw + 1, by + bh + 1, color);
        }
    }

    private static void drawRooms(GuiGraphicsExtractor ctx, Font font, DungeonMapOptions options,
                                  List<DungeonMapRoomData> rooms, float ox, float oy, float compToPx) {
        float roomRectOffset = (float) ((1.0 - options.roomWidth) * 0.5);
        float iconSizeComp = (float) (options.iconSize * options.roomWidth);
        float roomPx = (float) (options.roomWidth * compToPx);
        float textScale = Math.max(0.35f, (roomPx * 0.25f * (float) options.textSize) / 9.0f);

        for (DungeonMapRoomData data : rooms) {
            if (data == null) continue;
            DungeonRoom room = data.room;
            if (room == null) continue;
            if (room.doors.isEmpty() && room.name == null) continue;

            int color = colorForRoom(options, room);
            if ((color >>> 24) == 0) color = DungeonMapOptions.argb(options.color(MapColor.RoomNormal));

            ShapeType shape = room.shape;
            if (shape == ShapeType.Unknown) continue;

            List<WorldComponent> cells = new ArrayList<>(room.comps);
            boolean renderRoomInfo = true;

            if (!room.explored && !options.renderUnknownRooms) {
                shape = ShapeType.Shape1x1;
                cells = new ArrayList<>();
                for (WorldComponent comp : room.comps) {
                    int cx = comp.cx();
                    int cz = comp.cz();
                    boolean adjacent = room.doors.stream().anyMatch(door ->
                            Math.abs(cx - door.comp.cx()) + Math.abs(cz - door.comp.cz()) == 1
                                    && door.rooms.stream().anyMatch(other -> other.explored));
                    if (adjacent) cells.add(comp);
                }
                renderRoomInfo = false;
            }

            if (cells.isEmpty()) continue;

            if (shape == ShapeType.Shape2x2 && cells.size() != 4) shape = ShapeType.ShapeL;
            if (shape == ShapeType.ShapeL && cells.size() != 3) {
                shape = switch (cells.size()) {
                    case 1 -> ShapeType.Shape1x1;
                    case 2 -> ShapeType.Shape1x2;
                    default -> ShapeType.ShapeL;
                };
            }

            switch (shape) {
                case ShapeL -> {
                    for (int i = 0; i < cells.size(); i++) {
                        int cx = cells.get(i).cx() / 2;
                        int cz = cells.get(i).cz() / 2;
                        drawRoom(ctx, ox, oy, compToPx, roomRectOffset, options, cx, cz, 1, 1, color);
                        for (int j = i; j < cells.size(); j++) {
                            int cx2 = cells.get(j).cx() / 2;
                            int cz2 = cells.get(j).cz() / 2;
                            if (Math.abs(cx - cx2) + Math.abs(cz - cz2) == 1) {
                                drawJoined(ctx, ox, oy, compToPx, roomRectOffset, options, cx, cz, cx2, cz2, color);
                            }
                        }
                    }
                }
                case Shape1x1 -> {
                    Component comp = cells.get(0).toComponent();
                    List<RoomType> predictions =
                            options.showPredictions && comp.equals(data.predictedAt) ? data.predictedTypes : null;
                    int cx = cells.get(0).cx() / 2;
                    int cz = cells.get(0).cz() / 2;
                    int[] bounds = roomBounds(ox, oy, compToPx, roomRectOffset, options, cx, cz, 1, 1);

                    int x0 = bounds[0];
                    int y0 = bounds[1];
                    int w = bounds[2];
                    int h = bounds[3];

                    int count = predictions == null ? 0 : predictions.size();
                    if (count == 0) {
                        ctx.fill(x0, y0, x0 + w, y0 + h, color);
                    } else if (count == 1) {
                        ctx.fill(x0, y0, x0 + w, y0 + h, predictionColor(options, predictions.get(0), room));
                    } else if (count == 2) {
                        int halfW = w / 2;
                        ctx.fill(x0, y0, x0 + halfW, y0 + h, predictionColor(options, predictions.get(0), room));
                        ctx.fill(x0 + halfW, y0, x0 + w, y0 + h, predictionColor(options, predictions.get(1), room));
                    } else if (count == 3) {
                        int halfW = w / 2;
                        int halfH = h / 2;
                        ctx.fill(x0, y0, x0 + halfW, y0 + halfH, predictionColor(options, predictions.get(0), room));
                        ctx.fill(x0, y0 + halfH, x0 + halfW, y0 + h, predictionColor(options, predictions.get(1), room));
                        ctx.fill(x0 + halfW, y0, x0 + halfW + halfW, y0 + h,
                                predictionColor(options, predictions.get(2), room));
                    } else {
                        int halfW = w / 2;
                        int halfH = h / 2;
                        ctx.fill(x0, y0, x0 + halfW, y0 + halfH, predictionColor(options, predictions.get(0), room));
                        ctx.fill(x0, y0 + halfH, x0 + halfW, y0 + h, predictionColor(options, predictions.get(1), room));
                        ctx.fill(x0 + halfW, y0, x0 + w, y0 + halfH, predictionColor(options, predictions.get(2), room));
                        ctx.fill(x0 + halfW, y0 + halfH, x0 + w, y0 + h,
                                predictionColor(options, predictions.get(3), room));
                    }
                }
                case Shape1x2, Shape1x3, Shape1x4 -> {
                    WorldComponent corner = cells.stream()
                            .min((a, b) -> Integer.compare(a.cx() + a.cz(), b.cx() + b.cz()))
                            .orElse(cells.get(0));
                    int cx1 = cells.get(0).cx();
                    int cx2 = cells.get(1).cx();
                    int w = cx1 == cx2 ? 1 : cells.size();
                    int h = cx1 == cx2 ? cells.size() : 1;
                    drawRoom(ctx, ox, oy, compToPx, roomRectOffset, options, corner.cx() / 2, corner.cz() / 2,
                            w, h, color);
                }
                case Shape2x2 -> {
                    WorldComponent corner = cells.stream()
                            .min((a, b) -> Math.abs(a.cx() + a.cz()) - Math.abs(b.cx() + b.cz()))
                            .orElse(cells.get(0));
                    drawRoom(ctx, ox, oy, compToPx, roomRectOffset, options, corner.cx() / 2, corner.cz() / 2,
                            2, 2, color);
                }
                default -> {
                }
            }

            drawRoomInfo(ctx, font, options, room, cells, shape, ox, oy, compToPx,
                    iconSizeComp, textScale, renderRoomInfo);
        }
    }

    private static int[] roomBounds(float ox, float oy, float compToPx, float roomRectOffset,
                                    DungeonMapOptions options, int x, int z, int w, int h) {
        float fx = x + roomRectOffset;
        float fy = z + roomRectOffset;
        float fw = (float) (options.roomWidth + w - 1);
        float fh = (float) (options.roomWidth + h - 1);

        int bx = px(ox + fx * compToPx);
        int by = px(oy + fy * compToPx);
        int bw = (int) Math.ceil(fw * compToPx) + 1;
        int bh = (int) Math.ceil(fh * compToPx) + 1;
        return new int[]{bx, by, bw, bh};
    }

    private static void drawRoom(GuiGraphicsExtractor ctx, float ox, float oy, float compToPx, float roomRectOffset,
                                 DungeonMapOptions options, int x, int z, int w, int h, int color) {
        int[] bounds = roomBounds(ox, oy, compToPx, roomRectOffset, options, x, z, w, h);
        ctx.fill(bounds[0], bounds[1], bounds[0] + bounds[2], bounds[1] + bounds[3], color);
    }

    private static void drawJoined(GuiGraphicsExtractor ctx, float ox, float oy, float compToPx,
                                   float roomRectOffset, DungeonMapOptions options,
                                   int x1, int z1, int x2, int z2, int color) {
        if (x1 > x2 || z1 > z2) {
            int tx = x1;
            int tz = z1;
            x1 = x2;
            z1 = z2;
            x2 = tx;
            z2 = tz;
        }

        float fx;
        float fy;
        float fw;
        float fh;
        if (x1 == x2) {
            fx = x1 + roomRectOffset;
            fy = z1 + roomRectOffset + (float) options.roomWidth;
            fw = (float) options.roomWidth;
            fh = roomRectOffset * 2f;
        } else {
            fx = x1 + roomRectOffset + (float) options.roomWidth;
            fy = z1 + roomRectOffset;
            fw = roomRectOffset * 2f;
            fh = (float) options.roomWidth;
        }

        int bx = px(ox + fx * compToPx);
        int by = px(oy + fy * compToPx);
        int bw = (int) Math.ceil(fw * compToPx);
        int bh = (int) Math.ceil(fh * compToPx);
        ctx.fill(bx, by, bx + bw + 1, by + bh + 1, color);
    }

    private static void drawRoomInfo(GuiGraphicsExtractor ctx, Font font, DungeonMapOptions options,
                                     DungeonRoom room, List<WorldComponent> cells, ShapeType shape,
                                     float ox, float oy, float compToPx,
                                     float iconSizeComp, float textScale, boolean renderRoomInfo) {
        boolean renderName = renderRoomInfo
                && (!options.roomNameNotEFB
                || (room.type != RoomType.ENTRANCE && room.type != RoomType.FAIRY && room.type != RoomType.BLOOD))
                && (!options.roomNameNotYellow || room.type != RoomType.YELLOW)
                && (room.type == RoomType.PUZZLE ? options.puzzleName : options.roomName)
                && (!options.roomCheckGreen || room.checkmark != CheckmarkType.GREEN);

        String icon = puzzleIcon(options, room, renderRoomInfo);
        boolean puzzleIconShown = icon != null;
        if (icon == null) icon = checkmarkIcon(options, room, renderName);

        if (icon == null
                && options.renderCheckIf0Secret
                && room.totalSecrets == 0
                && (room.type == RoomType.YELLOW || room.type == RoomType.NORMAL || room.type == RoomType.BLOOD)
                && room.checkmark != CheckmarkType.UNEXPLORED
                && room.checkmark != CheckmarkType.NONE) {
            icon = "checks/" + ICON_STYLES[clampIconStyle(options.iconStyle)] + "/" + checkmarkFile(room.checkmark);
        }

        List<String> text = new ArrayList<>();
        // The icon already identifies the puzzle, so the name would only collide with it.
        if (renderName && room.name != null && !puzzleIconShown) {
            String colorCode = options.colorRoomName ? nameColorCode(room) : "";
            for (String word : room.name.replace("\u200B", "- ").split(" ")) {
                text.add(colorCode + word);
            }
        }

        if (renderRoomInfo
                && options.secretCount
                && room.totalSecrets > (options.no1Secret ? 1 : 0)
                && (!options.roomCheckGreen || room.checkmark != CheckmarkType.GREEN)) {
            String value;
            if (room.checkmark == CheckmarkType.GREEN) {
                value = String.valueOf(Math.max(room.totalSecrets, room.secretsCompleted));
            } else if (room.secretsCompleted < 0) {
                value = "?";
            } else {
                value = String.valueOf(room.secretsCompleted);
            }
            text.add(checkmarkColorCode(room) + value + "/" + room.totalSecrets);
        }

        if (icon == null && text.isEmpty()) return;

        float[] center = getCenterOf(cells, shape, options.textAlignment);

        if (icon != null) {
            float[] iconCenter = getCenterOf(cells, shape, options.iconAlignment);
            float decW = iconSizeComp;
            int bx = px(ox + (iconCenter[0] - decW * 0.5f) * compToPx);
            int by = px(oy + (iconCenter[1] - decW * 0.5f) * compToPx);
            int bw = (int) Math.ceil(decW * compToPx);
            int bh = (int) Math.ceil(decW * compToPx);
            blitIcon(ctx, icon, bx, by, bw, bh);
        }

        if (text.isEmpty()) return;

        float centerX = ox + center[0] * compToPx;
        float centerY = oy + center[1] * compToPx;

        float lineHeight = 10f * textScale;
        float totalHeight = lineHeight * text.size();
        float topY = centerY - totalHeight * 0.5f;

        Matrix3x2fStack pose = ctx.pose();
        pose.pushMatrix();
        pose.translate(centerX, topY);
        pose.scale(textScale, textScale);

        for (int i = 0; i < text.size(); i++) {
            String line = text.get(i);
            int lineWidth = font.width(line);
            drawText(ctx, font, line, -lineWidth / 2, i * 10, options.stringShadow);
        }

        pose.popMatrix();
    }

    /**
     * Puzzle rooms are identified by an icon instead of a name, so the icon wins over the
     * checkmark: a checkmark icon would otherwise hide it as soon as the room gets checked.
     */
    private static String puzzleIcon(DungeonMapOptions options, DungeonRoom room, boolean renderRoomInfo) {
        if (!options.puzzleIcon || !renderRoomInfo) return null;
        if (room.type != RoomType.PUZZLE || room.name == null) return null;

        String file = SPECIAL_ROOMS.get(room.name.trim());
        return file == null ? null : "puzzles/" + file;
    }

    private static String checkmarkIcon(DungeonMapOptions options, DungeonRoom room, boolean renderName) {
        boolean drawCheck = (options.checkMark
                || (renderName && room.name == null && room.checkmark != CheckmarkType.UNEXPLORED))
                && (!options.roomNoCheckName || !renderName || room.name == null)
                && (!options.roomNoFairyCheck
                || room.type != RoomType.FAIRY
                || room.checkmark == CheckmarkType.UNEXPLORED);
        if (!drawCheck) return null;
        if (options.renderUnknownRooms && room.checkmark == CheckmarkType.UNEXPLORED && room.name != null) return null;

        String file = checkmarkFile(room.checkmark);
        if (file == null) return null;
        return "checks/" + ICON_STYLES[clampIconStyle(options.iconStyle)] + "/" + file;
    }

    private static int clampIconStyle(int style) {
        return Math.max(0, Math.min(ICON_STYLES.length - 1, style));
    }

    private static String checkmarkFile(CheckmarkType checkmark) {
        return switch (checkmark) {
            case NONE -> null;
            case WHITE -> "whitecheck.png";
            case GREEN -> "greencheck.png";
            case FAILED -> "failedroom.png";
            case UNEXPLORED -> "questionmark.png";
        };
    }

    private static String nameColorCode(DungeonRoom room) {
        if (room.type == RoomType.ENTRANCE || room.type == RoomType.FAIRY) return "§f";
        if (room.type == RoomType.BLOOD) return room.checkmark == CheckmarkType.GREEN ? "§a" : "§f";
        return checkmarkColorCode(room);
    }

    private static String checkmarkColorCode(DungeonRoom room) {
        return switch (room.checkmark) {
            case FAILED -> "§c";
            case GREEN -> "§a";
            case WHITE -> "§f";
            case NONE -> "§7";
            case UNEXPLORED -> (room.explored || DungeonTracker.started) ? "§7" : "§f";
        };
    }

    private static float[] getCenterOf(List<WorldComponent> cells, ShapeType shape, RoomInfoAlignment alignment) {
        switch (alignment) {
            case TopLeft -> {
                WorldComponent best = cells.stream()
                        .min((a, b) -> Integer.compare(a.cx() + a.cz(), b.cx() + b.cz())).orElse(cells.get(0));
                return new float[]{best.cx() / 2f + 0.5f, best.cz() / 2f + 0.5f};
            }
            case TopRight -> {
                WorldComponent best = cells.stream()
                        .min((a, b) -> Integer.compare(-a.cx() + a.cz(), -b.cx() + b.cz())).orElse(cells.get(0));
                return new float[]{best.cx() / 2f + 0.5f, best.cz() / 2f + 0.5f};
            }
            case BottomLeft -> {
                WorldComponent best = cells.stream()
                        .min((a, b) -> Integer.compare(a.cx() - a.cz(), b.cx() - b.cz())).orElse(cells.get(0));
                return new float[]{best.cx() / 2f + 0.5f, best.cz() / 2f + 0.5f};
            }
            case BottomRight -> {
                WorldComponent best = cells.stream()
                        .min((a, b) -> Integer.compare(-a.cx() - a.cz(), -b.cx() - b.cz())).orElse(cells.get(0));
                return new float[]{best.cx() / 2f + 0.5f, best.cz() / 2f + 0.5f};
            }
            default -> {
                if (shape == ShapeType.ShapeL) {
                    List<WorldComponent> sorted = new ArrayList<>(cells);
                    sorted.sort((a, b) -> Integer.compare(a.cx() + a.cz() * 11, b.cx() + b.cz() * 11));

                    if (alignment == RoomInfoAlignment.Center) {
                        int idx;
                        if (sorted.get(0).cx() > sorted.get(1).cx()) idx = 2;
                        else if (sorted.get(0).cx() == sorted.get(2).cx()) idx = 0;
                        else idx = 1;
                        WorldComponent cell = sorted.get(idx);
                        return new float[]{cell.cx() / 2f + 0.5f, cell.cz() / 2f + 0.5f};
                    }

                    int idx = sorted.get(0).cz() == sorted.get(1).cz() ? 2 : 0;
                    float sumX = 0;
                    float sumZ = 0;
                    for (int i = 0; i < sorted.size(); i++) {
                        if (i == idx) continue;
                        sumX += sorted.get(i).cx() / 2f;
                        sumZ += sorted.get(i).cz() / 2f;
                    }
                    return new float[]{sumX / 2f + 0.5f, sumZ / 2f + 0.5f};
                }

                float sumX = 0;
                float sumZ = 0;
                for (WorldComponent cell : cells) {
                    sumX += cell.cx() / 2f;
                    sumZ += cell.cz() / 2f;
                }
                return new float[]{sumX / cells.size() + 0.5f, sumZ / cells.size() + 0.5f};
            }
        }
    }

    private static void drawPlayers(GuiGraphicsExtractor ctx, Font font, DungeonMapOptions options,
                                    FloorType floor, float maxDim, float ox, float oy, float compToPx,
                                    float hudScale) {
        List<DungeonPlayer> players = DungeonTracker.playerList();
        if (players.isEmpty()) return;

        boolean holdingLeap = isHoldingLeap();
        boolean renderNames = options.renderNames && (!options.namesRequireLeap || holdingLeap);

        // The local player is drawn last so their marker is never hidden behind a teammate.
        List<DungeonPlayer> ordered = new ArrayList<>(players);
        ordered.sort(Comparator.comparingInt(player -> isSelf(player) ? 1 : 0));

        for (DungeonPlayer player : ordered) {
            if (player.isDead) continue;

            PlayerComponent pos = player.getLerpedPosition();
            if (pos == null) continue;

            boolean isSelf = isSelf(player);

            float px = (float) MapMath.rescale(pos.x(), 0, maxDim * 2.0,
                    ox, ox + maxDim * compToPx);
            float py = (float) MapMath.rescale(pos.z(), 0, maxDim * 2.0,
                    oy, oy + maxDim * compToPx);

            boolean hasHead = options.usePlayerHeads && player.profileInfo != null;
            boolean isHead = hasHead && (!options.useMarkerSelf || !isSelf);

            float size;
            float maxDy;
            if (isHead) {
                size = (float) (4.0 * options.headScale * hudScale);
                maxDy = 4f;
            } else {
                size = (float) (2.0 * options.markerScale * hudScale);
                maxDy = 2.8f;
            }
            float height = isHead ? size : size * 1.4f;

            Matrix3x2fStack pose = ctx.pose();
            pose.pushMatrix();
            pose.translate(px, py);

            pose.rotate((float) (Math.PI * 0.5 - pos.r()));

            if (options.colorMarkerByClass && player.role != DungeonClass.Unknown
                    && player.role.color.getAlpha() != 0) {
                float[] outline = isHead ? MARKER_HEAD_OUTLINE : MARKER_POINTER_OUTLINE;
                float outlineW = height * 1.35f;
                float outlineH = isHead ? height * 1.35f : height * 1.55f;
                blitAtlas(ctx, outline, -outlineW * 0.5f, -outlineH * 0.5f, outlineW, outlineH,
                        player.role.colorRgb());
            }

            if (isHead) {
                drawHead(ctx, player, -size * 0.5f, -height * 0.5f, size, height);
            } else {
                blitAtlas(ctx, isSelf ? MARKER_SELF : MARKER_OTHER, -size * 0.5f, -height * 0.5f, size, height, -1);
            }

            pose.popMatrix();

            if (!renderNames) continue;

            String nameFormat = options.colorNameByClass ? player.role.colorCode : "";
            String text;
            if (holdingLeap) text = player.name;
            else if (options.useClassName && player.role != DungeonClass.Unknown) text = player.role.shortName;
            else text = player.name;

            float nameScale = hudScale * 0.3f * (float) options.nameScale;
            String line = nameFormat + text;
            float lineWidth = font.width(line) * nameScale;
            float nameY = py - maxDy * (isHead ? (float) options.headScale : (float) options.markerScale) * hudScale
                    - (9f * nameScale) * 0.5f;

            Matrix3x2fStack namePose = ctx.pose();
            namePose.pushMatrix();
            namePose.translate(px - lineWidth * 0.5f, nameY);
            namePose.scale(nameScale, nameScale);
            drawText(ctx, font, line, 0, 0, options.stringShadow);
            namePose.popMatrix();
        }
    }

    private static boolean isSelf(DungeonPlayer player) {
        if (mc.player != null && mc.player.getName().getString().equals(player.name)) return true;

        DungeonPlayer self = DungeonTracker.selfPlayer;
        return self != null && (self == player || self.name.equals(player.name));
    }

    private static boolean isHoldingLeap() {
        Player player = mc.player;
        if (player == null) return false;
        String id = HotbarSwapUtils.getSkyblockID(player.getMainHandItem());
        return "SPIRIT_LEAP".equals(id) || "INFINITE_SPIRIT_LEAP".equals(id);
    }

    private static void drawHead(GuiGraphicsExtractor ctx, DungeonPlayer player, float x, float y,
                                 float width, float height) {
        if (player.profileInfo == null) return;
        var skin = player.profileInfo.getSkin();
        if (skin == null || skin.body() == null) return;

        Identifier texture = skin.body().texturePath();
        int w = Math.round(width);
        int h = Math.round(height);

        try {
            ctx.blit(RenderPipelines.GUI_TEXTURED, texture, Math.round(x), Math.round(y),
                    8f, 8f, w, h, 8, 8, 64, 64, -1);
            ctx.blit(RenderPipelines.GUI_TEXTURED, texture, Math.round(x), Math.round(y),
                    40f, 8f, w, h, 8, 8, 64, 64, -1);
        } catch (Exception e) {
            warnOnce(texture.toString());
        }
    }

    private static void blitAtlas(GuiGraphicsExtractor ctx, float[] uv, float x, float y, float width, float height,
                                  int color) {
        try {
            ctx.blit(RenderPipelines.GUI_TEXTURED, MARKER_ATLAS,
                    Math.round(x), Math.round(y),
                    uv[0], uv[1],
                    Math.max(1, Math.round(width)), Math.max(1, Math.round(height)),
                    Math.round(uv[2]), Math.round(uv[3]),
                    MARKER_ATLAS_WIDTH, MARKER_ATLAS_HEIGHT,
                    color);
        } catch (Exception e) {
            warnOnce(MARKER_ATLAS.toString());
        }
    }

    private static void drawText(GuiGraphicsExtractor ctx, Font font, String text, int x, int y, TextShadow shadow) {
        switch (shadow) {
            case None -> ctx.text(font, text, x, y, 0xFFFFFFFF, false);
            case Drop -> ctx.text(font, text, x, y, 0xFFFFFFFF, true);
            case Outline -> {
                int dark = 0xFF000000;
                ctx.text(font, text, x - 1, y, dark, false);
                ctx.text(font, text, x + 1, y, dark, false);
                ctx.text(font, text, x, y - 1, dark, false);
                ctx.text(font, text, x, y + 1, dark, false);
                ctx.text(font, text, x, y, 0xFFFFFFFF, false);
            }
        }
    }

    private static void blitIcon(GuiGraphicsExtractor ctx, String icon, int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) return;

        Identifier texture = Identifier.tryParse("frosty:dungeons/map/" + icon);
        if (texture == null) {
            warnOnce("frosty:dungeons/map/" + icon);
            return;
        }

        try {
            ctx.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0f, 0f, width, height, width, height, -1);
        } catch (Exception e) {
            warnOnce(texture.toString());
        }
    }

    private static final java.util.Set<String> WARNED = new java.util.HashSet<>();

    private static void warnOnce(String texture) {
        if (!WARNED.add(texture)) return;
        System.err.println("Frosty: invalid dungeon map texture path '" + texture + "'");
    }

}
