package haven;

import java.util.LinkedList;
import java.util.List;

import static haven.OCache.*;

public class PathQueue {
    private static final boolean DBG = false;
    private final List<Coord2d> queue = new LinkedList<>();
    private final MapView map;
    private Moving moving;
    
    public PathQueue(MapView map) {
	this.map = map;
	CFG.QUEUE_PATHS.observe(cfg -> {
	    if(!cfg.get()) {clear();}
	});
    }
    
    public boolean add(Coord2d p) {
	boolean start = false;
	synchronized (queue) {
	    if(queue.isEmpty()) { start = true; }
	    queue.add(p);
	}
	
	return start;
    }
    
    public void start(Coord2d p) {
	synchronized (queue) {
	    queue.clear();
	    queue.add(p);
	}
    }
    
    public List<Pair<Coord3f, Coord3f>> lines() {
	LinkedList<Coord2d> tmp;
	synchronized (queue) {
	    tmp = new LinkedList<>(queue);
	}
	
	List<Pair<Coord3f, Coord3f>> lines = new LinkedList<>();
	if(!tmp.isEmpty()) {
	    try {
		Gob player = map.player();
		if(player != null) {
		    Coord3f current = moving == null ? player.getrc() : moving.gett();
		    for (Coord2d p : tmp) {
		        Coord3f next = map.glob.map.getzp(p);
			lines.add(new Pair<>(current, next));
			current = next;
		    }
		}
	    } catch (Loading ignored) {}
	}
	return lines;
    }
    
    private Coord2d pop() {
	synchronized (queue) {
	    if(queue.isEmpty()) { return null; }
	    queue.remove(0);
	    return queue.isEmpty() ? null : queue.get(0);
	}
    }
    
    public void movementChange(Gob gob, GAttrib from, GAttrib to) {
	if(skip(gob)) {return;}
	if(DBG) {log(gob, from, to);}
	moving = (Moving) to;
	synchronized (queue) {
	    if(to == null) {
		Coord2d next = pop();
		if(next != null) {
		    map.wdgmsg("click", Coord.z, next.floor(posres), 1, 0);
		}
	    } else if(to instanceof Homing || to instanceof Following) {
		clear();
	    }
	}
    }
    
    private boolean skip(Gob gob) {
	Gob me = map.player();
	if(me == null) {
	    boolean skip = map.plgob == -1 || map.plgob != gob.id;
	    if(DBG) Debug.log.printf("skip (%d) '%d' is null, %b%n", gob.id, map.plgob, skip);
	    return skip;
	}
	if(me.drives == 0) {
	    boolean skip = me.id != gob.id;
	    if(DBG) Debug.log.printf("skip (%d) '%d'<%d> not drives, %b%n", gob.id, map.plgob, me.drives, skip);
	    return skip;
	} else {
	    boolean skip = gob.id != me.drives;
	    if(DBG) Debug.log.printf("skip (%d) '%d'<%d> drives, %b%n", gob.id, map.plgob, me.drives, skip);
	    return skip;
	}
    }
    
    private void clear() {
	synchronized (queue) {queue.clear();}
    }
    
    private void log(Gob gob, GAttrib from, GAttrib to) {
	String type = "unknown";
	String action = "unknown";
	if(to == null) {
	    if(from != null) {
		action = "removed";
		type = from.getClass().getName();
	    } else {
		action = "removed";
		type = "both empty???";
	    }
	} else {
	    if(from != null) {
		action = "switched";
		type = from.getClass().getName() + " to " + to.getClass().getName();
	    } else {
		action = "added";
		type = to.getClass().getName();
	    }
	}
	Debug.log.printf("id:'%d' %s - %s%n", gob.id, action, type);
    }
}