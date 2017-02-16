/*
 *  This file is part of the Haven & Hearth game client.
 *  Copyright (C) 2009 Fredrik Tolf <fredrik@dolda2000.com>, and
 *                     Björn Johannessen <johannessen.bjorn@gmail.com>
 *
 *  Redistribution and/or modification of this file is subject to the
 *  terms of the GNU Lesser General Public License, version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  Other parts of this source tree adhere to other copying
 *  rights. Please see the file `COPYING' in the root directory of the
 *  source tree for details.
 *
 *  A copy the GNU Lesser General Public License is distributed along
 *  with the source tree of which this file is a part in the file
 *  `doc/LPGL-3'. If it is missing for any reason, please see the Free
 *  Software Foundation's website at <http://www.fsf.org/>, or write
 *  to the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 *  Boston, MA 02111-1307 USA
 */

package haven;

import me.ender.Reflect;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

public class Makewindow extends Widget {
    Widget obtn, cbtn;
    List<Spec> inputs = Collections.emptyList();
    List<Spec> outputs = Collections.emptyList();
    List<Indir<Resource>> qmod = null;
    static final Text qmodl = Text.render("Quality:");
    static Coord boff = new Coord(7, 9);
    final int xoff = 45, qmy = 38, outy = 65;
    private Tex qtex;
    private boolean rebuild = false;
    public static final Text.Foundry nmf = new Text.Foundry(Text.serif, 20).aa(true);

    @RName("make")
    public static class $_ implements Factory {
	public Widget create(Widget parent, Object[] args) {
	    return(new Makewindow((String)args[0]));
	}
    }
    
    public class Spec implements GSprite.Owner, ItemInfo.SpriteOwner {
	public Indir<Resource> res;
	public MessageBuf sdt;
	public Tex num;
	private GSprite spr;
	private Object[] rawinfo;
	private List<ItemInfo> info;

	public Spec(Indir<Resource> res, Message sdt, int num, Object[] info) {
	    this.res = res;
	    this.sdt = new MessageBuf(sdt);
	    if(num >= 0)
		this.num = new TexI(Utils.outline2(Text.render(Integer.toString(num), Color.WHITE).img, Utils.contrast(Color.WHITE)));
	    else
		this.num = null;
	    this.rawinfo = info;
	}

	public void draw(GOut g) {
	    try {
		if(spr == null)
		    spr = GSprite.create(this, res.get(), sdt.clone());
		spr.draw(g);
	    } catch(Loading e) {}
	    if(num != null)
		g.aimage(num, Inventory.sqsz, 1.0, 1.0);
	}

	public BufferedImage shorttip() {
	    List<ItemInfo> info = info();
	    if(info.isEmpty()) {
		Resource.Tooltip tt = res.get().layer(Resource.tooltip);
		if(tt == null)
		    return(null);
		return(Text.render(tt.t).img);
	    }
	    return(ItemInfo.shorttip(info()));
	}
	public BufferedImage longtip() {
	    List<ItemInfo> info = info();
	    BufferedImage img;
	    if(info.isEmpty()) {
		Resource.Tooltip tt = res.get().layer(Resource.tooltip);
		if(tt == null)
		    return(null);
		img = Text.render(tt.t).img;
	    } else {
		img = ItemInfo.longtip(info);
	    }
	    Resource.Pagina pg = res.get().layer(Resource.pagina);
	    if(pg != null)
		img = ItemInfo.catimgs(0, img, RichText.render("\n" + pg.text, 200).img);
	    return(img);
	}

	private Random rnd = null;
	public Random mkrandoom() {
	    if(rnd == null)
		rnd = new Random();
	    return(rnd);
	}
	public Resource getres() {return(res.get());}
	public Glob glob() {return(ui.sess.glob);}

	public List<ItemInfo> info() {
	    if(info == null)
		info = ItemInfo.buildinfo(this, rawinfo);
	    return(info);
	}
	public Resource resource() {return(res.get());}
	public GSprite sprite() {return(spr);}
    }
	
    public void tick(double dt) {
	for(Spec s : inputs) {
	    if(s.spr != null)
		s.spr.tick(dt);
	}
	for(Spec s : outputs) {
	    if(s.spr != null)
		s.spr.tick(dt);
	}
    }

    public Makewindow(String rcpnm) {
	add(new Label("Input:"), new Coord(0, 8));
	add(new Label("Result:"), new Coord(0, outy + 8));
	obtn = add(new Button(85, "Craft"), new Coord(230, 75));
	cbtn = add(new Button(85, "Craft All"), new Coord(325, 75));
	pack();
	adda(new Label(rcpnm, nmf), sz.x, 0, 1, 0);
    }
	
    public void uimsg(String msg, Object... args) {
	if(msg == "inpop") {
	    List<Spec> inputs = new LinkedList<Spec>();
	    for(int i = 0; i < args.length;) {
		int resid = (Integer)args[i++];
		Message sdt = (args[i] instanceof byte[])?new MessageBuf((byte[])args[i++]):MessageBuf.nil;
		int num = (Integer)args[i++];
		Object[] info = {};
		if((i < args.length) && (args[i] instanceof Object[]))
		    info = (Object[])args[i++];
		inputs.add(new Spec(ui.sess.getres(resid), sdt, num, info));
	    }
	    this.inputs = inputs;
	} else if(msg == "opop") {
	    List<Spec> outputs = new LinkedList<Spec>();
	    for(int i = 0; i < args.length;) {
		int resid = (Integer)args[i++];
		Message sdt = (args[i] instanceof byte[])?new MessageBuf((byte[])args[i++]):MessageBuf.nil;
		int num = (Integer)args[i++];
		Object[] info = {};
		if((i < args.length) && (args[i] instanceof Object[]))
		    info = (Object[])args[i++];
		outputs.add(new Spec(ui.sess.getres(resid), sdt, num, info));
	    }
	    this.outputs = outputs;
	} else if(msg == "qmod") {
	    List<Indir<Resource>> qmod = new ArrayList<Indir<Resource>>();
	    for(Object arg : args)
		qmod.add(ui.sess.getres((Integer)arg));
	    this.qmod = qmod;
	    this.rebuild = true;
	} else {
	    super.uimsg(msg, args);
	}
    }
	
    public void draw(GOut g) {
	Coord c = new Coord(xoff, 0);
	for(Spec s : inputs) {
	    GOut sg = g.reclip(c, Inventory.invsq.sz());
	    sg.image(Inventory.invsq, Coord.z);
	    s.draw(sg);
	    c = c.add(Inventory.sqsz.x, 0);
	}
	if(qmod != null) {
	    g.image(qmodl.tex(), new Coord(0, qmy + 4));
	    if(rebuild) {
		buildQTex();
	    }
	    if(qtex != null) {
		g.image(qtex, new Coord(xoff, qmy));
	    }
	}
	c = new Coord(xoff, outy);
	for(Spec s : outputs) {
	    GOut sg = g.reclip(c, Inventory.invsq.sz());
	    sg.image(Inventory.invsq, Coord.z);
	    s.draw(sg);
	    c = c.add(Inventory.sqsz.x, 0);
	}
	super.draw(g);
    }
    
    private void buildQTex(){
	rebuild = false;
	double product = 1.0d;
	int count = 0;
	BufferedImage result = null;
	for (Indir<Resource> qm : qmod) {
	    try {
		result = ItemInfo.catimgsh(8, result, qm.get().layer(Resource.imgc).img);
		try {
		    Glob.CAttr attr = ui.gui.chrwdg.findattr(qm.get().basename());
		    if(attr != null) {
			result = ItemInfo.catimgsh(1, result, attr.compline().img);
			product = product * attr.comp;
			count++;
		    }
		} catch (Exception ignored) {
		    rebuild = true;
		}
	    } catch (Loading l) {
		rebuild = true;
	    }
	}
    
	if(count > 0) {
	    double softcap = Math.pow(product, 1.0d / count);
	    String format = String.format("Softcap: %.1f", softcap);
	    Text txt = Text.renderstroked(format, Color.WHITE, Color.BLACK, Glob.CAttr.fnd);
	    result = ItemInfo.catimgsh(16, result, txt.img);
	}
    
	if(result != null) {
	    qtex = new TexI(result);
	} else {
	    qtex = null;
	}
    }
    
    private long hoverstart;
    private Spec lasttip;
    private Indir<Object> stip, ltip;
    public Object tooltip(Coord mc, Widget prev) {
	String name = null;
	Spec tspec = null;
	Coord c;
	if(qmod != null) {
	    c = new Coord(xoff, qmy);
	    try {
		for(Indir<Resource> qm : qmod) {
		    Tex t = qm.get().layer(Resource.imgc).tex();
		    Coord sz = t.sz();
		    try {
			Glob.CAttr attr = ui.gui.chrwdg.findattr(qm.get().basename());
			if(attr != null) {
			    sz = sz.add(attr.compline().sz().x + 8, 0);
			}
		    }catch (Exception ignored){}
		    if(mc.isect(c, sz))
			return(qm.get().layer(Resource.tooltip).t);
		    c = c.add(sz.x + 1, 0);
		}
	    } catch(Loading l) {
	    }
	}
	find: {
	    c = new Coord(xoff, 0);
	    for(Spec s : inputs) {
		if(mc.isect(c, Inventory.invsq.sz())) {
		    name = getDynamicName(s.spr);
		    if(name == null){
			tspec = s;
		    }
		    break find;
		}
		c = c.add(31, 0);
	    }
	    c = new Coord(xoff, outy);
	    for(Spec s : outputs) {
		if(mc.isect(c, Inventory.invsq.sz())) {
		    tspec = s;
		    break find;
		}
		c = c.add(31, 0);
	    }
	}
	if(lasttip != tspec) {
	    lasttip = tspec;
	    stip = ltip = null;
	}
	if(tspec == null)
	    return(name);
	long now = System.currentTimeMillis();
	boolean sh = true;
	if(prev != this)
	    hoverstart = now;
	else if(now - hoverstart > 1000)
	    sh = false;
	if(sh) {
	    if(stip == null) {
		BufferedImage tip = tspec.shorttip();
		if(tip == null) {
		    stip = () -> null;
		} else {
		    Tex tt = new TexI(tip);
		    stip = () -> tt;
		}
	    }
	    return(stip);
	} else {
	    if(ltip == null) {
		BufferedImage tip = tspec.longtip();
		if(tip == null) {
		    ltip = () -> null;
		} else {
		    Tex tt = new TexI(tip);
		    ltip = () -> tt;
		}
	    }
	    return(ltip);
	}
    }

    private static String getDynamicName(GSprite spr) {
	if(spr != null) {
	    Class<? extends GSprite> sprClass = spr.getClass();
	    if(Reflect.hasInterface("haven.res.ui.tt.defn.DynName", sprClass)) {
		return (String) Reflect.invoke(spr, "name");
	    }
	}
	return null;
    }

    public void wdgmsg(Widget sender, String msg, Object... args) {
	if(sender == obtn) {
	    if(msg == "activate")
		wdgmsg("make", 0);
	    return;
	}
	if(sender == cbtn) {
	    if(msg == "activate")
		wdgmsg("make", 1);
	    return;
	}
	super.wdgmsg(sender, msg, args);
    }
    
    public boolean globtype(char ch, java.awt.event.KeyEvent ev) {
	if(ch == '\n') {
	    wdgmsg("make", ui.modctrl?1:0);
	    return(true);
	}
	return(super.globtype(ch, ev));
    }
    
    public static class MakePrep extends ItemInfo implements GItem.ColorInfo {
	private final static Color olcol = new Color(0, 255, 0, 64);
	public MakePrep(Owner owner) {
	    super(owner);
	}
	
	public Color olcol() {
	    return(olcol);
	}
    }
}
