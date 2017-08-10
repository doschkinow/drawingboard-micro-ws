package com.mycompany.drawingboard.light;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.websocket.EncodeException;
import javax.websocket.Session;
import javax.ws.rs.core.MultivaluedHashMap;

/**
 * Simple in-memory data storage for the application.
 */
class DataProvider {

    /**
     * Map that stores drawings by ID.
     */
    private static final HashMap<Integer, Drawing> drawings
            = new HashMap<>();

    /**
     * Map that stores web socket sessions corresponding to a given drawing ID.
     */
    private static final MultivaluedHashMap<Integer, Session> webSockets
            = new MultivaluedHashMap<>();

    /**
     * Retrieves a drawing by ID.
     *
     * @param drawingId ID of the drawing to be retrieved.
     * @return Drawing with the corresponding ID.
     */
    static synchronized Drawing getDrawing(int drawingId) {
        Drawing drawing = drawings.get(drawingId);
        if (drawing == null) {
            drawing = new Drawing();
            drawings.put(drawingId, drawing);
        }
        return drawing;
    }

    /**
     * Add a new shape to the drawing.
     *
     * @param drawingId ID of the drawing the shape should be added to.
     * @param shape Shape to be added to the drawing.
     * @return {@code true} if the shape was added, {@code false} if no such
     * drawing was found.
     */
    static synchronized boolean addShape(int drawingId, Drawing.Shape shape) {
        Drawing drawing = getDrawing(drawingId);
        if (drawing != null) {
            if (drawing.shapes == null) {
                drawing.shapes = new ArrayList<>();
            }
            drawing.shapes.add(shape);
            wsBroadcast(drawingId, shape);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Registers a new web socket session and associates it with a drawing ID.
     * This method should be called when a client opens a web socket connection
     * to a particular drawing URI.
     *
     * @param drawingId Drawing ID to associate the web socket session with.
     * @param session New web socket session to be registered.
     */
    static synchronized void addWebSocket(int drawingId, Session session) {
        webSockets.add(drawingId, session);
        try {
            Optional<String> loc = Optional.ofNullable(System.getenv("SSE_LOCATION"));
            session.getBasicRemote().sendText("{\"sseLocation\":" + "\"" + loc.orElse("localhost:8080") + "\"" + "}");
            Drawing drawing = getDrawing(drawingId);
            if (drawing != null && drawing.shapes != null) {
                for (Drawing.Shape shape : drawing.shapes) {
                    session.getBasicRemote().sendObject(shape);
                }
            }
        } catch (IOException | EncodeException ex) {
            Logger.getLogger(DataProvider.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    /**
     * Removes the existing web socket session associated with a drawing ID.
     * This method should be called when a client closes the web socket
     * connection to a particular drawing URI.
     *
     * @param drawingId ID of the drawing the web socket session is associated
     * with.
     * @param session Web socket session to be removed.
     */
    static synchronized void removeWebSocket(int drawingId, Session session) {
        List<Session> sessions = webSockets.get(drawingId);
        if (sessions != null) {
            sessions.remove(session);
        }
    }

    /**
     * Broadcasts the newly added shape to all web sockets associated with the
     * affected drawing.
     *
     * @param drawingId ID of the affected drawing.
     * @param shape Shape that was added to the drawing or
     * {@link ShapeCoding#SHAPE_CLEAR_ALL} if the drawing was cleared (i.e. all
     * shapes were deleted).
     */
    private static void wsBroadcast(int drawingId, Drawing.Shape shape) {
        List<Session> sessions = webSockets.get(drawingId);
        if (sessions != null) {
            for (Session session : sessions) {
                try {
                    session.getBasicRemote().sendObject(shape);
                } catch (IOException | EncodeException ex) {
                    Logger.getLogger(DataProvider.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }
}
