module experiments::vis2::sandbox::FigureServer
import experiments::vis2::sandbox::Figure;
import experiments::vis2::sandbox::IFigure;
import Prelude;

public void render(Figure fig1, int width = 400, int height = 400, 
     Alignment align = <0.5, 0.5>, tuple[int, int] size = <0, 0>,
     str fillColor = "white", str lineColor = "black", bool debug = false, bool display = true)
     {
     setDebug(debug);
     _render(fig1, width = width,  height = height,  align = align, fillColor = fillColor,
     lineColor = lineColor, size = size);
     // println(toString());
     }
       
public str toHtmlString(Figure fig1, int width = 400, int height = 400, 
     Alignment align = <0.5, 0.5>, tuple[int, int] size = <0, 0>,
     str fillColor = "white", str lineColor = "black", bool debug = false)
     {
     setDebug(debug);
     _render(fig1, width = width,  height = height,  align = align, fillColor = fillColor,
     lineColor = lineColor, size = size, display = false);
     // return "aap";
     return getIntro();
     }


public Style style(str id, str fillColor="", str lineColor="", int lineWidth = -1) {
     Style v = _getStyle(id);
     v.svg = isSvg(id);
     if (lineWidth!=-1) v.lineWidth = lineWidth;
     if (!isEmpty(fillColor)) v.fillColor = fillColor;
     if (!isEmpty(lineColor)) v.lineColor = lineColor;
     _setStyle(id, v);
     return v;
     }
     
public Attr attr(str id, int width = -1, int height = -1, int width = -1, int r = -1) {
     Attr v = _getAttr(id);
     if (width!=-1) v.width = width;
     if (height!=-1) v.height = height;
     if (r!=-1) v.r = r;
     _setAttr(id, v);
     return v;
     }

public Text textLabel(str id, str text = "") {
     Text v = _getText(id);
     if (!isEmpty(text)) v.text = text;
     _setText(id, v);
     //  println(v);
     return v;
     }

