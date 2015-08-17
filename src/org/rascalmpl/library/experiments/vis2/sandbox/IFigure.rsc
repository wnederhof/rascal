@license{
  Copyright (c) 2009-2015 CWI
  All rights reserved. This program and the accompanying materials
  are made available under the terms of the Eclipse Public License v1.0
  which accompanies this distribution, and is available at
  http://www.eclipse.org/legal/epl-v10.html
}
@contributor{Bert Lisser - Bert.Lisser@cwi.nl (CWI)}
@contributor{Paul Klint - Paul.Klint@cwi.nl - CWI}

module experiments::vis2::sandbox::IFigure
import Prelude;
import util::Webserver;

import lang::json::IO;
import util::HtmlDisplay;
import util::Math;
import experiments::vis2::sandbox::Figure;

private loc base = |std:///experiments/vis2/sandbox|;

// The random accessable data element by key id belonging to a widget, like _box, _circle, _hcat. 

alias Elm = tuple[void(str, str) f, int seq, str id, str begintag, str endtag, str script, int width, int height,
      int x, int y, int lx, int ly, 
      Alignment align, int lineWidth, str lineColor, bool sizeFromParent, bool svg];

// Map which stores the widget info

public map[str, Elm] widget = (); 

// The tree which is the compiled Figure, the only fields are id or content.
// Id is a reference to hashmap widget

public data IFigure = ifigure(str id, list[IFigure] child);

public data IFigure = ifigure(str content);

public data IFigure = iemptyFigure();

// --------------------------------------------------------------------------------

bool debug = true;
int screenWidth = 400;
int screenHeight = 400;

int seq = 0;
int occur = 0;

// ----------------------------------------------------------------------------------
alias State = tuple[str name, Prop v];
public list[State] state = [];
public list[Prop] old = [];
//-----------------------------------------------------------------------------------

public map[str, str] parentMap = ();

public map[str, Figure] figMap = ();

public list[str] widgetOrder = [];

public list[str] adjust = [];

public list[str] googleChart = [];

public list[str] markerScript = [];

public map[str, list[IFigure] ] defs = ();

IFigure fig;

// ----------------------------------------------------------------------------------------

public void setDebug(bool b) {
   debug = b;
   }
   
public bool isSvg(str id) = widget[id].svg;

public void null(str event, str id ){return;}

Attr _getAttr(str id) = state[widget[id].seq].v.attr;

void _setAttr(str id, Attr v) {state[widget[id].seq].v.attr = v;}

Style _getStyle(str id) = state[widget[id].seq].v.style;

void _setStyle(str id, Style v) {state[widget[id].seq].v.style = v;}

Text _getText(str id) = state[widget[id].seq].v.text;

void _setText(str id, Text v) {state[widget[id].seq].v.text = v;}

void addState(Figure f) {
    Attr attr = attr();
    Style style = style(fillColor=getFillColor(f)
       ,lineColor =getLineColor(f), lineWidth = getLineWidth(f));
    Text text = text();
    Prop prop = <attr, style, text>;
    seq=seq+1;
    state += <f.id, prop >;
    old+= prop;
    }
      
public void clearWidget() { 
    println("clearWidget <screenWidth> <screenHeight>");
    widget = (); widgetOrder = [];adjust=[]; googleChart=[];
    markerScript = [];
    defs=(); 
    parentMap=(); figMap = ();
    seq = 0; occur = 0;
    }
              
str visitFig(IFigure fig) {
    if (ifigure(str id, list[IFigure] f):= fig) {
         return 
    "<widget[id].begintag> <for(d<-f){><visitFig(d)><}><widget[id].endtag>\n";
         }
    if (ifigure(str content):=fig) return content;
    return "";
    }
    
str visitDefs(str id, bool orient) {
    if (defs[id]?) {
     for (f<-defs[id]) {
         // markerScript+= "alert(<getWidth(f)>);";
         markerScript+= "d3.select(\"#m_<f.id>\")
         ' <attr1("markerWidth", getWidth(f)+2)>
         ' <attr1("markerHeight", getHeight(f)+2)>
         ' ;
         "
         ;       
         }
     return "\<defs\>
           ' <for (f<-defs[id]){> \<marker id=\"m_<f.id>\"        
           ' refX = <orient?getWidth(f)/2:0>   refY = <orient?getHeight(f)/2:0> <orient?"orient=\"auto\"":"">
           ' \> 
           ' <visitFig(f)>
           ' \</marker\> <}>
           '\</defs\>
           ";
    }
    return "";
    }
 

str google = "\<script src=\'https://www.google.com/jsapi?autoload={
        ' \"modules\":[{
        ' \"name\":\"visualization\",
        ' \"version\":\"1\"
        ' }]
        '}\'\> \</script\>"
;     
       
str getIntro() {
   // println(widgetOrder);
   res = "\<html\>
        '\<head\>      
        '\<style\>
        'body {
        '    font: 300 14px \'Helvetica Neue\', Helvetica;
        ' }
        '.node rect {
        ' stroke: #333;
        ' fill: #fff;
       '}
       '.edgePath path {
       ' stroke: #333;
       ' fill: #333;
       ' stroke-width: 1.5px;
       '  }
        '\</style\>    
        '\<script src=\"IFigure.js\"\>\</script\>
        '\<script src=\"http://d3js.org/d3.v3.min.js\" charset=\"utf-8\"\>\</script\>        
        '\<script src=\"http://cpettitt.github.io/project/dagre-d3/latest/dagre-d3.min.js\"\>\</script\>
        '<google> 
        '\<script\>
        '  function doFunction(id) {
        '    return function() {
        '    askServer(\"<getSite()>/getValue/\"+id, {},
        '            function(t) {
        '                for (var d in t) {
        '                   var e = d3.select(\"#\"+d); 
        '                   for (var i in t[d][\"text\"]) {
        '                        e=e.text(t[d][\"text\"][i]);
        '                        }
        '                   for (var i in t[d][\"attr\"]) {
        '                        e=e.attr(i, t[d][\"attr\"][i]);
        '                        }
        '                   var svg = t[d][\"style\"][\"svg\"];
        '                   for (var i in t[d][\"style\"]) {
        '                        e=e.style(svgStyle(i, svg), t[d][\"style\"][i]);
        '                        }    
        '                   // e.text(t[d]);
        '                  // e.style(\"background\", \"\"+t[d]);
        '                   }
        '                });  
        '   };
        ' }
       ' function initFunction() {
           <for (d<-markerScript) {> <d> <}>
           <for (d<-widgetOrder) {> <widget[d].script> <}>
           <for (d<-reverse(adjust)) {> <d> <}>
           <for (d<-googleChart) {> <d> <}>        
       ' }
       ' onload=initFunction;
       '\</script\>
       '\</head\>
       '\<body\>
       ' <visitFig(fig)>      
       '\</body\>     
		'\</html\>
		";
    println(res);
	return res;
	}
	
   

Response page(get(), /^\/$/, map[str,str] _) { 
	return response(getIntro());
}

bool eqProp(Prop p, Prop q) = p == q;
   
list[State] diffNewOld() {
    return [state[i]|i<-[0..size(state)], !eqProp(state[i].v, old[i])];
    }
    
map[str, value] makeMap(Prop p) {
   map[str, value] attr = getKeywordParameters(p.attr);
   map[str, value] style = getKeywordParameters(p.style);
   map[str, value] text = getKeywordParameters(p.text);
   return ("attr":attr, "style":style, "text":text);
   }

Response page(post(), /^\/getValue\/<name:[a-zA-Z0-9_]+>/, map[str, str] parameters) {
	// println("post: getValue: <name>, <parameters>");
	widget[name].f("click", name);
	list[State] changed = diffNewOld();
	map[str, Prop] c = toMapUnique(changed);
	map[str, map[str, value]] d = (s:makeMap(c[s])|s<-c);
	// println(d);
	str res = toJSON(d, true);
	// println(res);
	old = [s.v|s<-state];
	return response("<res>");
}

default Response page(get(), str path, map[str, str] parameters) {
   //println("File response: <base+path>");
   return response(base + path); 
   }

private loc startFigureServer() {
  	loc site = |http://localhost:8081|;
  
  while (true) {
    try {
      //println("Trying ... <site>");
      serve(site, dispatchserver(page));
      return site;
    }  
    catch IO(_): {
      site.port += 1; 
    }
  }
}

private loc site = startFigureServer();

private str getSite() = "<site>"[1 .. -1];


      
public void _render(IFigure fig1, int width = 400, int height = 400, 
     Alignment align = centerMid, int lineWidth = -1, 
     str fillColor = "none", str lineColor = "black", bool display = true)
     {
     screenWidth = width;
     screenHeight = height;
     str id = "figureArea";
     
    str begintag= beginTag(id, align);
    // println(getWidth(fig1));
    str endtag = endTag();    
    widget[id] = <null, seq, id, begintag, endtag, 
        "
        'd3.select(\"#<id>\")<style("border","1px solid black")> 
        '<stylePx("width", width)><style("height", width)>
        '<style("stroke-width",lineWidth)>
        ;       
        "
       , width, height, width, height, 0, 0, align, 1, "", false, false >;
      
       widgetOrder += id;
    fig = ifigure(id, [fig1]);
    println("site=<site>");
	if (display) htmlDisplay(site);
}

str getId(IFigure f) {
    if (ifigure(id, _) := f) return id;
    return "";
    }
    
void setId(Figure f) {  
    if (isEmpty(f.id)) f.id = "i<id>";
    id = id +1;    
    }

int getWidth(IFigure f) {
    if (ifigure(id, _) := f) return widget[id].width;
    return -1;
    }
    
int getHeight(IFigure f) {
    if (ifigure(id, _) := f) return widget[id].height;
    return -1;
    }

int getLineWidth(IFigure f) {
    if (ifigure(id, _) := f) return widget[id].lineWidth;
    return -1;
    }
    
int getLineWidth(Figure f) {
    int lw = f.lineWidth;
    while (lw<0) {
        f = figMap[parentMap[f.id]];
        lw = f.lineWidth;
        }
   return lw;
  }
  
str getLineColor(Figure f) {
    str c = f.lineColor;
    while (isEmpty(c)) {
        f = figMap[parentMap[f.id]];
        c = f.lineColor;
        }
   return c;
  }

str getFillColor(Figure f) {
    str c = f.fillColor;
    while (isEmpty(c)) {
        f = figMap[parentMap[f.id]];
        c = f.fillColor;
        }
   return c;
  } 
   
num getFillOpacity(Figure f) {
    num c = f.fillOpacity;
    while (c<0) {
        f = figMap[parentMap[f.id]];
        c = f.fillOpacity;
        }
   return c;
  }
  
num getLineOpacity(Figure f) {
    num c = f.lineOpacity;
    while (c<0) {
        f = figMap[parentMap[f.id]];
        c = f.lineOpacity;
        }
   return c;
  }
    
bool getSizeFromParent(IFigure f) {
    if (ifigure(id, _) := f) return widget[id].sizeFromParent;
    return false;
    }

int getX(IFigure f) {
    if (ifigure(id, _) := f) return widget[id].x;
    return -1;
    }
    
int getY(IFigure f) {
    if (ifigure(id, _) := f) return widget[id].y;
    return -1;
    }
    
int getHgap(IFigure f) {
    if (ifigure(id, _) := f) return widget[id].hgap;
    return -1;
    }
    
int getVgap(IFigure f) {
    if (ifigure(id, _) := f) return widget[id].vgap;
    return -1;
    }
    
Alignment getAlign(IFigure f) {
    if (ifigure(id, _) := f) return widget[id].align;
    return <-1, -1>;
    } 
    
void(str, str) getCallback(Event e) {
    if (on(_, void(str, str) callback):=e) return callback;
    return null; 
    }
       
str getEvent(Event e) {
    if (on(str eventName, _):=e) return eventName;
    return ""; 
    }
    
str debugStyle() {
    if (debug) return style("border","1px solid black");
    return "";
    }

str borderStyle(Figure f) {
      return 
      "<style("border-width",f.borderWidth)> 
      '<style("border-style",f.borderStyle)>
      '<style("border-color",f.borderColor)>";
     }
    
str style(str key, str v) {
    if (isEmpty(v)) return "";
    return ".style(\"<key>\",\"<v>\")";
    }
    
str style(str key, num v) {
    if (v<0) return "";
    return ".style(\"<key>\",\"<v>\")";
    }

str stylePx(str key, int v) {
    if (v<0) return "";
    return ".style(\"<key>\",\"<v>px\")";
    }
    
str attrPx(str key, int v) {
    if (v<0) return "";
    return ".attr(\"<key>\",\"<v>px\")";
    }  
       
str attr(str key, str v) {
    if (isEmpty(v)) return "";
    return ".attr(\"<key>\",\"<v>\")";
    }

str attr1(str key, str v) {
    if (isEmpty(v)) return "";
    return ".attr(\"<key>\",<v>)";
    }    

str attr(str key, int v) {
    if (v<0) return "";
    return ".attr(\"<key>\",\"<v>\")";
    }
    
str attr1(str key, int v) {
    if (v<0) return "";
    return ".attr(\"<key>\",<v>)";
    }
    
str attr(str key, real v) {
    if (v<0) return "";
    return ".attr(\"<key>\",\"<precision(v, 1)>\")";
    } 
        
str on(str ev, str proc) {
    if (isEmpty(ev)) return "";
    return ".on(\"<ev>\", <proc>)";
    }         
// -----------------------------------------------------------------------
// '.text(\"<s>\") 
int getTextWidth(Figure f, str s) {
     if (f.width>=0) return f.width;
     int fw =  f.fontSize<0?12:f.fontSize;
     return size(s)*fw;
     }
 
int getTextHeight(Figure f) {
   if (f.height>=0) return f.height;
   int fw =  (f.fontSize<0?12:f.fontSize)+5;
   return fw;
   }
   
int getTextX(Figure f, str s) {
     int fw =  (f.fontSize<0?12:f.fontSize);
     if (f.width>=0) {    
         return (f.width-size(s)*fw)/2;
         }
     return fw/2;
     }

int getTextY(Figure f) {
     int fw =  (f.fontSize<0?12:f.fontSize);
     fw += fw/2;
     if (f.height>=0) return f.height/2+fw;    
     return fw;
     }  
          
IFigure _text(str id, bool fo, Figure f, str s) {
    str begintag=fo?"\<div  id=\"<id>\"\>":
    "\<svg <moveAt(fo, f)> id=\"<id>_svg\"\> \<text id=\"<id>\"\>";
    int width = f.width;
    int height = f.height;
    Alignment align =  width<0?topLeft:f.align;
    // str endtag = addSvgTag?"\</text\>":"\</div\>"; 
    str endtag = fo?"\</div\>":"\</text\>\</svg\>";
    // 
    widget[id] = <null, seq, id, begintag, endtag, 
        "
        'd3.select(\"#<id>\")
        '<debugStyle()>
        '<fo?style("background-color", "<f.fillColor>"):"">
        '<stylePx("width", width)><stylePx("height", height)>
        '<stylePx("font-size", f.fontSize)>
        '<style("font-style", f.fontStyle)>
        '<style("font-family", f.fontFamily)>
        '<style("font-weight", f.fontWeight)>
        '<style("color", f.fontColor)>
        '<fo?"":attr("y", getTextY(f))+attr("x", getTextX(f, s))> 
        '.text(\"<s>\") 
        ';"
        , getTextWidth(f, s), getTextHeight(f), getAtX(f), getAtY(f), 0, 0, align, getLineWidth(f), getLineColor(f), f.sizeFromParent, true >;
       addState(f);
       widgetOrder+= id;
       return ifigure(id, []);
    }
    
str trChart(str cmd, str id, Figure chart) {
    // println(chart);
    map[str,value] kw = getKeywordParameters(chart);
    ChartOptions options = chart.options;
    str d = ""; 
    if ((kw["charts"]?) && !isEmpty(chart.charts)) {
      list[Chart] charts = chart.charts;
      d = 
      toJSON(joinData(charts, chart.tickLabels, chart.tooltipColumn), true);
      options = updateOptions(charts, options);
      }
    if ((kw["googleData"]?) && !isEmpty(chart.googleData)) 
       d = toJSON(chart.googleData, true);
    if ((kw["xyData"]?) && !isEmpty(chart.xyData)) {     
       d = toJSON([["x", "y"]] + [[e[0], e[1]]|e<-chart.xyData], true);
       }  
    if (options.width>=0) chart.width = options.width;
    if (options.height>=0) chart.height = options.height;
    return 
    "{
     '\"chartType\": \"<cmd>\",
     '\"containerId\":\"<id>\",
    ' \"options\": <adt2json(options)>,
    ' \"dataTable\": <d>  
    '}";   
    //  <propsToJSON(chart, parent)> 
    }
    
str drawVisualization(str fname, str json) { 
    return "
    'function <fname>() {
    'var wrap = new google.visualization.ChartWrapper(
    '<json>
    ');
    'wrap.draw();
    '}
    ";  
    }
    
str vl(value v) {
    if (str s:=v) return "\"<v>\"";
    return v;
    }
    
IFigure _googlechart(str cmd, str id, Figure f) {
    str begintag="\<div  id=\"<id>\"\>";
    int width = f.width;
    int height = f.height;
    Alignment align =  width<0?topLeft:f.align;
    str endtag = "\</div\>"; 
    // println(drawVisualization(id, trChart("ComboChart", id, f)));
    str fname = "googleChart_<id>";
    googleChart+="<fname>();\n";
    widget[id] = <null, seq, id, begintag, endtag, 
        "
        'd3.select(\"#<id>\")
        '<debugStyle()>
        '<style("background-color", "<getFillColor(f)>")>
        '<stylePx("width", width)><stylePx("height", height)>
        ';
        '<drawVisualization(fname, trChart(cmd, id, f))>
        "
        , f.width, f.height, 0, 0, 0, 0, align, getLineWidth(f), getLineColor(f), f.sizeFromParent, false >;
       addState(f);
       widgetOrder+= id;
       return ifigure(id, []);
    }
    
 str getDotOpt(Figure f, str s, Figure g) {
    str r =  (emptyFigure():=g)?",{":",{shape:\"<g.id>\",label:\"\"";
    if ((f.nodeProp[s]?)) {
        map[str, value] m = getKeywordParameters(f.nodeProp[s]);
        for (t<-m) {
           r+=",<t>:<vl(m[t])>,";
           }       
        }
    r+="}";
    return r;
    }
    
str getGraphOpt(Figure g) {
   map[str, value] m = getKeywordParameters(g.options);
   str r =  "{style:\"fill:none\", ";
   if (!isEmpty(m)) {
        for (t<-m) {
           r+="<t>:<vl(m[t])>,";
           }    
        r = replaceLast(r,",","");                
        }
     r+="}";
    return r;
    }
    
str getEdgeOpt(Edge s) {
      map[str, value] m = getKeywordParameters(s);
      str r =  ",{";
      // str r =  ", {style:\"fill:none\", ";
      if (s.lineInterpolate=="basis") {
          r+="lineInterpolate:\"basis\",";
          }
      if (!isEmpty(m))  {  
         for (t<-m) {
           if (t!="lineColor")
           r+="<t>:<vl(m[t])>,";
           else r+="style: \"stroke:<m[t]>;fill:none\",";
           }    
         r = replaceLast(r,",","");
         } 
         r+="}";         
    return r;
    }
    
 str trGraph(Figure g) {
    // for (s<-g.nodes) println(s[1].id);
    // str r = getGraphOpt(g);
    str r = "var g = new dagreD3.graphlib.Graph().setGraph(<getGraphOpt(g)>);";
    r +="
     '<for(s<-g.nodes){> g.setNode(\"<s[0]>\"<getDotOpt(g, s[0], s[1])>);<}>
     ";
    r+="
     '<for(s:edge(from, to)<-g.edges)
     {>g.setEdge(\"<from>\", \"<to>\"<getEdgeOpt(s)>);<}>
     ";
    // println("r=<r>");
    r+="
    '<for(s<-g.nodes){> <emptyFigure():=s[1]?"":addShape(s[1])> <}>
    ";
    return r;
    }
    
str dagreIntersect(Figure s) {
    return "return dagreD3.intersect.polygon(node, points, point);";    
    }
    
str dagrePoints(f) {
      if (q:ngon():=f) {
          num angle = 2 * PI() / f.n;
          lrel[real, real] p  = [<f.r+f.r*cos(i*angle), f.r+f.r*sin(i*angle)>|int i<-[0..f.n]];
          return 
            replaceLast( "[<for(z<-p){> {x:<toInt(z[0])>, y: <toInt(z[1])>},<}>]",
          ",", "");
          }
       return "[{x:0, y:0}, {x:width, y:0}, {x:width, y:height}, {x:0, y:height}]";
       }
       
str addShape(Figure s) {  
    // println("addShape: <s.width>  <s.height>"); 
    int n = 4;
    switch(s) {
         case q:ngon(): n = q.n;
         case q:circle: n = 32;
         }
    return "
    'render.shapes().<s.id> = function (parent, bbox, node) {
    'var width = parseInt(d3.select(\"#<s.id>\").attr(\"width\"));
    'var height = parseInt(d3.select(\"#<s.id>\").attr(\"height\"));
    'var points = <dagrePoints(s)>;
    'var dpoints = \"M \"+ points.map(function(d) { return d.x + \" \" + d.y; }).join(\" \")+\" Z\";
    'shapeSvg = parent.insert(\"path\", \":first-child\")
    ' <attr1("d", "dpoints")> 
    ' <style("fill", "none")><style("stroke", "none")>
    ' <attr1("transform", "\"translate(\"+-width/2+\",\"+-height/2+\")\"")>
    '  
    '    node.intersect = function(point) {
    '    <dagreIntersect(s)>
    '    }
    '    return shapeSvg;
    '    }  
    ";
    
    // <style("marker-start","url(#m_<s.id>)")>;
    }
   
 str drawGraph(str fname, str id, str body) { 
    return "
    'function <fname>() {
    'var svg = d3.select(\"#<id>\");
    'var inner = svg.append(\"g\"); 
    'var render = new dagreD3.render();
  
    '<body>
    'render(inner, g);
    
    ' g.nodes().forEach(function(v) {
    '     var n = g.node(v);
    '     var id = n.shape;
    '     var width = parseInt(d3.select(\"#\"+id).attr(\"width\"));
    '     var height = parseInt(d3.select(\"#\"+id).attr(\"height\"));
    '    d3.select(\"#\"+id+\"_svg\")<attr1("x", "Math.floor(n.x-width/2)")><attr1("y", "Math.floor(n.y-height/2)")>;
    '     });
    '}
    "; 
    }
      
    
 IFigure _graph(str id, Figure f, list[str] ids) {
    //str begintag="\<div  id=\"<id>\"\>";
    str begintag =
         "\<svg id=\"<id>\"\><visitDefs(id, false)>";
    str endtag="\</svg\>";
    // str endtag = "";
    int width = f.width;
    int height = f.height;
    Alignment align =  width<0?topLeft:f.align;
    // str endtag = "\</div\>"; 
    str fname = "graph_<id>";
    // println(drawGraph(fname, id, trGraph(f)));
    googleChart+="<fname>();\n";
    
    widget[id] = <null, seq, id, begintag, endtag, 
        " 
        'd3.select(\"#<id>\")
        '<attrPx("width", width)><attrPx("height", height)>
        ';     
        '<drawGraph(fname, id, trGraph(f))>
        "
        , f.width, f.height, 0, 0, 0, 0, align, getLineWidth(f), getLineColor(f), f.sizeFromParent, true >;
       addState(f);
       widgetOrder+= id;
       return ifigure(id, []);
    }
    
str beginTag(str id, Alignment align) {
    return "
           '\<table  cellspacing=\"0\" cellpadding=\"0\" id=\"<id>\"\>\<tr\>
           ' \<td <vAlign(align)> <hAlign(align)>\>";
    }
  
str beginTag(str id, bool foreignObject, Alignment align, IFigure fig, int offset1, int offset2) {  
   str r =  foreignObject?"
    '\<foreignObject  id=\"<id>_fo\" x=\"<getAtX(fig)+offset1>\" y=\"<getAtY(fig)+offset2>\"
          width=\"<screenWidth>px\" height=\"<screenHeight>px\"\> 
    '<beginTag("<id>_fo_table", align)>
    "       
    :"";
    return r;
 }
 
str beginTag(str id, bool foreignObject, Alignment align, IFigure fig)
 = beginTag(id, foreignObject, align, fig, 0, 0);
 

str endTag(bool foreignObject) {
   str r = foreignObject?"<endTag()>\</foreignObject\>":"";
   return r;
   }
    
str endTag() {
   return "\</td\>\</tr\>\</table\>"; 
   } 
         
int getAtX(Figure f) {
         return toInt(f.at[0]);
         }
         
int getAtY(Figure f) {
         return toInt(f.at[1]);
         }
          
int getAtX(IFigure f) {
    if (ifigure(id, _) := f) return widget[id].x;
    return 0;
    }
    
int getAtY(IFigure f) {
    if (ifigure(id, _) := f) return widget[id].y;
    return 0;
    } 

int getLx(IFigure f) {
    if (ifigure(id, _) := f) return widget[id].lx;
    return 0;
    }
    
int getLy(IFigure f) {
    if (ifigure(id, _) := f) return widget[id].ly;
    return 0;
    }  
   
str beginRotate(Figure f) {
    
    if (f.rotate[0]==0) return "";
    if (f.rotate[1]<0 && f.rotate[2]<0) {  
        f.rotate[1] = (f.width)/2;
        f.rotate[2] = (f.height)/2;
        // println("cx: <f.rotate[1]>  cy: <f.rotate[2]>");
        return "\<g transform=\" rotate(<f.rotate[0]>,<f.rotate[1]>,<f.rotate[2]>)\" \>";
        }
    }
  
str endRotate(Figure f) =  f.rotate[0]==0?"":"\</g\>";

str beginScale(Figure f) {   
    if (f.grow==1) return "";
    return "\<g transform=\"scale(<f.grow>)\" \>";
    }
  
str endScale(Figure f) =  f.grow==1?"":"\</g\>";
    
int hPadding(Figure f) = f.padding[0]+f.padding[2];     

int vPadding(Figure f) = f.padding[1]+f.padding[3]; 

bool hasInnerCircle(Figure f)  {
     if (!((box():=f) || (ellipse():=f) || (circle():=f) || (ngon():=f))) return false;
     f =  f.fig;
     while (at(_, _, Figure g):= f|| atX(_,Figure g):=f || atY(_,Figure g):=f) {
          f = g;
          }
     return (circle():=f) || (ellipse():=f) || (ngon():=f);
     }
  
 str moveAt(bool fo, Figure f) = fo?"":"x=<getAtX(f)> y=<getAtY(f)>";
 
            
 IFigure _rect(str id, bool fo, Figure f,  IFigure fig = iemptyFigure(), Alignment align = <0, 0>) {    
      int lw = getLineWidth(f);  
      if (getAtX(fig)>0 || getAtY(fig)>0) f.align = topLeft;
      if ((f.width<0 || f.height<0) && (getWidth(fig)<0 || getHeight(fig)<0)) 
                      f.align = centerMid;
      int offset1 = round((0.5-f.align[0])*lw);
      int offset2 = round((0.5-f.align[1])*lw);
      if (getWidth(fig)>0 && getHeight(fig)>0){
           if (f.width<0) f.width = getWidth(fig)+lw+getAtX(fig)+hPadding(f);
           if (f.height<0) f.height = getHeight(fig)+lw+getAtY(fig)+vPadding(f);
           }
      // println("<id>: align = <lw> <f.align> <0.5-f.align[0]>  <offset> getAtX(fig)=<getAtX(fig)>");  
      str begintag= 
         "\<svg <moveAt(fo, f)> xmlns = \'http://www.w3.org/2000/svg\' id=\"<id>_svg\"\> <beginScale(f)> <beginRotate(f)> 
         '\<rect id=\"<id>\" /\> 
         '<beginTag("<id>", fo, f.align, fig, offset1, offset2)>
         "; 
       //  vector-effect=\"non-scaling-stroke\"    
       str endtag =endTag(fo);
       endtag += endRotate(f);  
       endtag+=endScale(f);
       endtag += "\</svg\>"; 
       int width = f.width;
       int height = f.height;
       int lx = 0;
       int ly = 0;   
       widget[id] = <getCallback(f.event), seq, id, begintag, endtag, 
        "
        'd3.select(\"#<id>\")
        '<on(getEvent(f.event), "doFunction(\"<id>\")")>
        '<attr("x", 0)><attr("y", 0)> 
        '<attr("rx", f.rounded[0])><attr("ry", f.rounded[1])> 
        '<attr("width", width)><attr("height", height)>
        '<styleInsideSvg(id, f, fig)>
        ",toInt(f.grow*f.width), toInt(f.grow*f.height), getAtX(f), getAtY(f), lx, ly, f.align, 
          toInt(f.grow*getLineWidth(f)), getLineColor(f), f.sizeFromParent, true >;
       addState(f);
       widgetOrder+= id;
       return ifigure(id, [fig]);
       } 
 
 str styleInsideSvgOverlay(str id, Figure f) {
      return " 
        '<style("stroke-width",getLineWidth(f))>
        '<style("stroke","<getLineColor(f)>")>
        '<style("fill", "<getFillColor(f)>")> 
        '<style("stroke-dasharray", cv(f.lineDashing))> 
        '<style("fill-opacity", getFillOpacity(f))> 
        '<style("stroke-opacity", getLineOpacity(f))>      
        ';       
        'd3.select(\"#<id>_svg\")
        '<attr("width", f.grow*f.width)><attr("height", f.grow*f.height)>     
        '<!isEmpty(f.tooltip)?
        ".append(\"svg:title\").text(\""+f.tooltip+"\")":"">
        ';
        ";
      } 
      
 str cv(list[int] ld) {
      if (isEmpty(ld)) return "";
      str r = "<head(ld)> <for(d<-tail(ld)){> , <d> <}>";
      return r;
      }
 
 str styleInsideSvg(str id, Figure f,  IFigure fig) {  
    int x  =getAtX(fig);
    int y = getAtX(fig);
    int lw = getLineWidth(f);
    int width = f.width;
    int height = f.height;  
    str bId = id; 
    switch (f) {
        case ngon():bId = "<id>_rect";
        case ellipse():bId = "<id>_rect";
        case circle():bId = "<id>_rect";
        }  
    int hpad = hPadding(f);
    int vpad = vPadding(f);    
    return styleInsideSvgOverlay(id, f) +
        "
        'd3.select(\"#<id>_svg\")
        '<attr("width", toInt(f.grow*width))><attr("height", toInt(f.grow*height))>
        ';
        
        'd3.select(\"#<id>_fo\")
        '<attr("width", width)><attr("height", height)>
        '<debugStyle()>
        ';    
        '       
        'd3.select(\"#<id>_fo_table\")
        '<style("width", width)><style("height", height)>
        '<_padding(f.padding)> 
        '<debugStyle()>
        ';
        "
        + ((!isEmpty(getId(fig))&& f.width<0)?"adjust0(\"<id>\", \"<getId(fig)>\", <lw>, <hpad>, <vpad>);\n":"");     
      }

num rxL(num rx, num ry) = rx * sqrt(rx*rx+ry*ry)/ry;

num ryL(num rx, num ry) = ry * sqrt(rx*rx+ry*ry)/rx;
         
num cxL(Figure f) =  
      (((ellipse():=f)?(f.rx):(f.r)) + (getLineWidth(f)>=0?(getLineWidth(f))/2.0:0));
num cyL(Figure f) =  
      (((ellipse():=f)?(f.ry):(f.r)) + (getLineWidth(f)>=0?(getLineWidth(f))/2.0:0));
     
 IFigure _ellipse(str id, bool fo, Figure f,  IFigure fig = iemptyFigure(), Alignment align = <0.5, 0.5>) {
      int lw = getLineWidth(f);    
      str tg = "";
      switch (f) {
          case ellipse(): {tg = "ellipse"; 
                           if (f.width>=0 && f.rx<0) f.rx = (f.width-lw)/2;
                           if (f.height>=0 && f.ry<0) f.ry = (f.height-lw)/2;
                           if (f.rx<0 || f.ry<0 || getAtX(fig)>0 || getAtY(fig)>0) f.align = centerMid; 
                           bool bx  = false;          
                           if (f.rx<0 && getWidth(fig)>=0) {
                              f.rx = getAtX(fig)+(getWidth(fig)+lw+hPadding(f))/2.0;
                              bx = true;
                              }
                           bool by  = false; 
                           if (f.ry<0 && getHeight(fig)>=0) {
                              f.ry = getAtY(fig)+(getHeight(fig)+lw+vPadding(f))/2.0;
                              by = true;
                              }
                           if (!hasInnerCircle(f)&& (bx || by)) { 
                              num rx = f.rx; num ry = f.ry;
                              if (bx) {
                                  rx -= hPadding(f);
                                  f.rx = rxL(rx, ry);
                                  f.rx+= hPadding(f);
                                  }
                              if (by) {
                                  ry -= vPadding(f);
                                  f.ry = ryL(rx, ry);
                                  f.ry+= vPadding(f);
                                  }
                              }
               
                           if (f.width<0 && f.rx>=0) f.width= round(f.rx*2+lw);
                           if (f.height<0 && f.ry>=0) f.height = round(f.ry*2+lw);                         
                           }
          case circle(): {tg = "circle";
                          if (f.width>=0 && f.height>=0 && f.r<0) 
                                       f.r = (max[f.width, f.height]-lw)/2;
                          if (f.r<0 || getAtX(fig)>0 || getAtY(fig)>0) f.align = centerMid; 
                          bool b  = false;             
                          int d = max([getWidth(fig), getHeight(fig)]);
                          if (f.r<0 && d>=0) {
                               f.r = max([getAtX(fig),getAtY(fig)])+
                                     (d+lw+max([hPadding(f), vPadding(f)]))/2.0;
                               b = true;
                               }
                          if (!hasInnerCircle(f)&& b) { 
                              num r = f.r - max([hPadding(f), vPadding(f)]);
                              f.r = rxL(r, r)+ max([hPadding(f), vPadding(f)]);
                              }
                          if (f.width<0 && f.r>=0) f.width= round(f.r*2+lw);
                          if (f.height<0 && f.r>=0) f.height = round(f.r*2+lw);                 
                          }
          } 
        if (f.cx>0 || f.cy>0) {
           int x = f.at[0]+round(f.cx)-round((ellipse():=f)?f.rx:f.r)-lw/2;
           int y=  f.at[1]+round(f.cy)-round((ellipse():=f)?f.ry:f.r)-lw/2;
           f.at = <x, y>;
           }  
       str begintag =
         "\<svg <moveAt(fo, f)> xmlns = \'http://www.w3.org/2000/svg\' id=\"<id>_svg\"\><beginScale(f)><beginRotate(f)>\<rect id=\"<id>_rect\"/\> \<<tg> id=\"<id>\"/\> 
         '<beginTag("<id>", fo, f.align, fig)>
         ";
       str endtag = endTag(fo);
       endtag += "<endRotate(f)><endScale(f)>\</svg\>"; 
       int width = f.width;
       int height = f.height;
       widget[id] = <getCallback(f.event), seq, id, begintag, endtag, 
        "
        'd3.select(\"#<id>_rect\")
        '<style("fill","none")><style("stroke", debug?"black":"none")><style("stroke-width", 1)>
        '<attr("x", 0)><attr("y", 0)><attr("width", f.width)><attr("height", f.height)>
        ;
        'd3.select(\"#<id>\")
        '<on(getEvent(f.event), "doFunction(\"<id>\")")>
        '<attr("cx", toP(cxL(f)))><attr("cy", toP(cyL(f)))> 
        '<attr("width", f.width)><attr("height", f.height)>
        '<ellipse():=f?"<attr("rx", toP(f.rx))><attr("ry", toP(f.ry))>":"<attr("r", toP(f.r))>">
        '<styleInsideSvg(id, f, fig)>
        ", f.width, f.height, getAtX(f), getAtY(f), 0, 0, f.align, getLineWidth(f), getLineColor(f)
         , f.sizeFromParent, true >;
       addState(f);
       widgetOrder+= id;
       return ifigure(id, [fig]);
       }
       
num rescale(num d, Rescale s) = s[1][0] + (d-s[0][0])*(s[1][1]-s[1][0])/(s[0][1]-s[0][0]);
       
str toP(num d, Rescale s) {
        num e = rescale(d, s);
        // println("toP:<s>  <d> -\> <e>");
        num v = abs(e);
        return "<toInt(e)>.<toInt(v*10)%10><toInt(v*100)%10>";
        }
        
str toP(num d) {
        if (d<0) return "";
        return toP(d, <<0,1>, <0, 1>>);
        }
      
str translatePoints(Figure f, Rescale scaleX, Rescale scaleY) {
       Points p;
       if (polygon():=f) {
           p = f.points;         
       }
       if (ngon():=f) {
            int lw = corner(f)/2;
             num angle = 2 * PI() / f.n;
             p  = [<f.r+lw+f.r*cos(i*angle), f.r+lw+f.r*sin(i*angle)>|int i<-[0..f.n]];
             }
       return "<toP(p[0][0], scaleX)>, <toP(p[0][1], scaleY)>" + 
            "<for(t<-tail(p)){> <toP(t[0], scaleX)> , <toP(t[1], scaleY)> <}>";
       }
    

    
str extraCircle(str id, Figure f) {
       if (ngon():=f) {
            return "\<circle id=\"<id>_circle\"/\>";
            }
       return "";
       } 
       
int corner(Figure f) {
     return corner(f.n, getLineWidth(f));
    }
    
int corner(int n, int lineWidth) {
     num angle = PI() - 2 * PI() / n;
     int lw = lineWidth<0?0:lineWidth;
     return toInt(lw/sin(0.5*angle));
    }  
   

num rR(Figure f)  = ngon():=f?f.r+corner(f)/2:-1;


int getNgonWidth(Figure f, IFigure fig) {
         if (f.width>=0) return f.width;
         int r = toInt(rR(f));
         // int lw = f.lineWidth<0?0:2;
         int lw = 1;
         if (ngon():=f) return 2*r+lw;     
         return -1;
         }

int getNgonHeight(Figure f, IFigure fig) {
         if (f.height>=0) return f.height;
         int r = toInt(rR(f));
         int lw = 1;  
         if (ngon():=f) return 2*r+lw;      
         return -1;
         }

int getPolWidth(Figure f) {
         if (f.width>=0) return f.width;
         num width = rescale(max([p.x|p<-f.points]), f.scaleX)+getLineWidth(f);  
         return toInt(width);
         }

int getPolHeight(Figure f) {
         if (f.height>=0) return f.height;
         num height = rescale(max([p.y|p<-f.points]), f.scaleY)+getLineWidth(f);     
         return toInt(height);
         }
       
IFigure _polygon(str id, Figure f,  IFigure fig = iemptyFigure()) {
       // str begintag= beginTag("<id>_table", topLeft); 
       f.width = getPolWidth(f);
       f.height = getPolHeight(f);
       if (f.yReverse) f.scaleY = <<0, f.height>, <f.height, 0>>;
       str begintag = "";
       begintag+=
         "\<svg id=\"<id>_svg\"\>\<polygon id=\"<id>\"/\>       
         ";
       str endtag = "\</svg\>"; 
       widget[id] = <getCallback(f.event), seq, id, begintag, endtag, 
        "  
        'd3.select(\"#<id>\")
        '<on(getEvent(f.event), "doFunction(\"<id>\")")>
        '<attr("points", translatePoints(f, f.scaleX, f.scaleY))>
        '<style("fill-rule", f.fillEvenOdd?"evenodd":"nonzero")>
        '<styleInsideSvgOverlay(id, f)>
        ", f.width, f.height, getAtX(f), getAtY(f),  0, 0, f.align, getLineWidth(f), getLineColor(f)
         , f.sizeFromParent, true >;
       addState(f);
       widgetOrder+= id;
       return ifigure(id, [fig]);
       }
       
 
num xV(Vertex v) = (line(num x, num y):=v)?x:((move(num x, num y):=v)?x:0);

num yV(Vertex v) = (line(num x, num y):=v)?y:((move(num x, num y):=v)?y:0);

int getPathWidth(Figure f) {
        if (shape(list[Vertex] vs):= f) {
             if (f.width>=0) return f.width;
             return screenWidth;
            }
         return -1;
         }

int getPathHeight(Figure f) {
         if (shape(list[Vertex] vs):= f) {
             if (f.height>=0) return f.height;
             return screenHeight;
         }
      }
      
str trVertices(Figure f) {
     if (shape(list[Vertex] vertices):=f) {
       return trVertices(f.vertices, shapeClosed= f.shapeClosed, shapeCurved = f.shapeCurved, shapeConnected= f.shapeConnected,
       scaleX = f.scaleX, scaleY=f.scaleY);
       }
     return emptyFigure;
     }
      
str trVertices(list[Vertex] vertices, bool shapeClosed = false, bool shapeCurved = true, bool shapeConnected = true,
    Rescale scaleX=<<0,1>, <0, 1>>, Rescale scaleY=<<0,1>, <0, 1>>) {
	//<width, height>  = bbox(vertices
	str path = "M<toP(vertices[0].x, scaleX)> <toP(vertices[0].y, scaleY)>"; // Move to start point
	int n = size(vertices);
	if(shapeConnected && shapeCurved && n > 2){
	    // println("OKOK");
		path += "Q<toP((vertices[0].x + vertices[1].x)/2.0, scaleX)> <toP((vertices[0].y + vertices[1].y)/2.0, scaleY)> <toP(vertices[1].x, scaleX)> <toP(vertices[1].y, scaleY)>";
		for(int i <- [2 ..n]){
			v = vertices[i];
			path += "<isAbsolute(v) ? "T" : "t"><toP(v.x, scaleX)> <toP(v.y, scaleY)>"; // Smooth point on quadartic curve
		}
	} else {
		for(int i <- [1 .. n]){
			v = vertices[i];
			path += "<directive(v)><toP(v.x, scaleX)> <toP(v.y, scaleY)>";
		}
	}
	
	if(shapeConnected && shapeClosed) path += "Z";
	
	return path;		   
}

bool isAbsolute(Vertex v) = (getName(v) == "line" || getName(v) == "move");

str directive(Vertex v) = ("line": "L", "lineBy": "l", "move": "M", "moveBy": "m")[getName(v)];

str mS(Figure f, str v) = ((emptyFigure():=f)?"": v);
       
IFigure _shape(str id, Figure f,  IFigure fig = iemptyFigure()) {
       // str begintag= beginTag("<id>_table", topLeft); 
       num top = 0, bottom = screenHeight, left = 0, right = screenHeight;
       if (shape(list[Vertex] vs):= f) {
           top = min([yV(p)|p<-vs]);
           bottom = max([yV(p)|p<-vs]);
           left = min([xV(p)|p<-vs]);
           right = max([xV(p)|p<-vs]);
       }   
       if (f.height<0)
            f.height = toInt(bottom-top)+ 100;
       if (f.width<0) 
             f.width = toInt(right-left)+ 100;
       if (abs(f.scaleX[1][1]-f.scaleX[1][0])>f.width) 
            f.width = toInt(abs(f.scaleX[1][1]-f.scaleX[1][0]));
       if (abs(f.scaleY[1][1]-f.scaleY[1][0])>f.height)
           f.height = toInt(abs(f.scaleY[1][1]-f.scaleY[1][0]));
       if (f.yReverse && f.scaleY==<<0,1>,<0,1>>) f.scaleY = <<0, f.height>, <f.height, 0>>;  
       str begintag = "";
       begintag+=
         "\<svg id=\"<id>_svg\"\><visitDefs(id, true)>\<path id=\"<id>\"/\>       
         ";
       str endtag = "\</svg\>"; 
       widget[id] = <getCallback(f.event), seq, id, begintag, endtag, 
        "
        'd3.select(\"#<id>\")
        '<on(getEvent(f.event), "doFunction(\"<id>\")")>
        '<attr("d", "<trVertices(f)>")> 
        '<style("marker-start", mS(f.startMarker, "url(#m_<id>_start)"))>
        '<style("marker-mid", mS(f.midMarker, "url(#m_<id>_mid)"))>
        '<style("marker-end",  mS(f.endMarker,"url(#m_<id>_end)"))>
        '<style("fill-rule", f.fillEvenOdd?"evenodd":"nonzero")>
        '<styleInsideSvgOverlay(id, f)>
        ", f.width, f.height, getAtX(f), getAtY(f), 0, 0, f.align, getLineWidth(f), getLineColor(f)
         , f.sizeFromParent, true >;
       addState(f);
       widgetOrder+= id;
       return ifigure(id, [fig]);
       }
       
str beginRotateNgon(Figure f) {
    
    if (f.rotate[0]==0) return "";
    if (f.rotate[1]<0 && f.rotate[2]<0) {
        f.rotate[1] = f.width/2;
        f.rotate[2]=  f.height/2;
        // println(f.rotate);
        return "\<g transform=\"rotate(<f.rotate[0]>,<f.rotate[1]>,<f.rotate[2]>)\" \>";
        }
    }
                 
IFigure _ngon(str id, bool fo, Figure f,  IFigure fig = iemptyFigure(), Alignment align = <0.5, 0.5>) {
       int lw = getLineWidth(f);
       if (f.r<0 || getAtX(fig)>0 || getAtY(fig)>0 || f.rotate[0]!=0) f.align = centerMid;
       bool b  = false;             
       int d = max([getWidth(fig), getHeight(fig)]);
       if (f.r<0 && d>=0) {
                f.r = max([getAtX(fig),getAtY(fig)])+
                (d+lw+max([hPadding(f), vPadding(f)]))/2.0;
                b = true;
                }
       if (!hasInnerCircle(f)&& b) { 
             num r = f.r - max([hPadding(f), vPadding(f)]);
             f.r = rxL(r, r);
             f.r += max([hPadding(f), vPadding(f)]);
             }
       if (f.width<0 && f.r>=0) f.width= round(f.r*2+lw);
       if (f.height<0 && f.r>=0) f.height = round(f.r*2+lw);
       str begintag = "";
       begintag+=
         "\<svg <moveAt(fo, f)> id=\"<id>_svg\"\><beginScale(f)><beginRotateNgon(f)>\<rect id=\"<id>_rect\"/\> <extraCircle(id, f)>\<polygon id=\"<id>\"/\>        
         '<beginTag("<id>", fo, f.align, fig)>
         ";
       str endtag =  endTag(fo); 
       endtag += "<endRotate(f)><endScale(f)>\</svg\>"; 
       widget[id] = <getCallback(f.event), seq, id, begintag, endtag, 
        "
        'd3.select(\"#<id>_rect\")
        '<style("fill","none")><style("stroke", debug?"black":"none")><style("stroke-width", 1)>
        '<attr("x", 0)><attr("y", 0)><attr("width", f.width)><attr("height", f.height)>
        ;
        'd3.select(\"#<id>_circle\")
        '<style("fill","none")><style("stroke", debug?"black":"none")><style("stroke-width", 1)>
        '<attr("cx", toP(cxL(f)))><attr("cy", toP(cyL(f)))><attr("r", toP(f.r))>
        
        'd3.select(\"#<id>\")
        '<on(getEvent(f.event), "doFunction(\"<id>\")")>
        '<attr("points", translatePoints(f, f.scaleX, f.scaleY))> 
        '<attr("width", f.width)><attr("height", f.height)>
        '<styleInsideSvg(id, f, fig)>
        ", f.width, f.height, getAtX(f), getAtY(f),  0, 0, f.align, getLineWidth(f), getLineColor(f)
         , f.sizeFromParent, true >;
       addState(f);
       widgetOrder+= id;
       return ifigure(id, [fig]);
       }
 
       
str vAlign(Alignment align) {
       if (align == bottomLeft || align == bottomMid || align == bottomRight) return "valign=\"bottom\"";
       if (align == centerLeft || align ==centerMid || align ==centerRight)  return "valign=\"middle\"";
       if (align == topLeft || align == topMid || align == topRight) return "valign=\"top\"";
       }
str hAlign(Alignment align) {
       if (align == bottomLeft || align == centerLeft || align == topLeft) return "align=\"left\"";
       if (align == bottomMid || align == centerMid || align == topMid) return "align=\"center\"";
       if (align == bottomRight || align == centerRight || align == topRight) return "align=\"right\"";    
       }
       
str _padding(tuple[int, int, int, int] p) {
       return stylePx("padding-left", p[0])+stylePx("padding-top", p[1])
             +stylePx("padding-right", p[2])+stylePx("padding-bottom", p[3]);     
       }

bool isSvg(str s) =  startsWith(s, "\<svg");
   
IFigure _overlay(str id, Figure f, IFigure fig1...) {
       int lw = getLineWidth(f)<0?0:getLineWidth(f); 
       // if (f.lineWidth<0) f.lineWidth = 0;  
       if (f.width<0 && min([getWidth(g)|g<-fig1])>=0) f.width = max([getAtX(g)+getWidth(g)|g<-fig1]);
       if (f.height<0 && min([getHeight(g)|g<-fig1])>=0) f.height = max([getAtY(g)+getHeight(g)|g<-fig1]);
       str begintag =
         "\<svg id=\"<id>_svg\"\><beginScale(f)><beginRotate(f)>\<rect id=\"<id>\"/\>";
       str endtag="<endRotate(f)><endScale(f)>\</svg\>";
       int width = f.width;
       int height = f.height;
       int lx = 0;
       int ly = 0;  
         //   '\<p\>\</p/\>
        widget[id] = <null, seq, id, begintag, endtag, 
        "
        'd3.select(\"#<id>\") 
        '<attr("width", f.width)><attr("height", f.height)>  
        '<styleInsideSvgOverlay(id, f)>    
        ';
        <for (q<-fig1){> 
        'd3.select(\"#<getId(q)>_svg\")<attrPx("x", getAtX(q)-getLx(q))><attrPx("y", getAtY(q)-getLy(q))>   
        ;<}> 
        <for (q<-fig1){> 
         '<getSizeFromParent(q)?"adjustFrame(\"<getId(q)>\", <f.width>, <f.height>);":"">
         <}>  
        "
        , f.width, f.height, getAtX(f), getAtY(f), lx, ly, f.align, getLineWidth(f), getLineColor(f), f.sizeFromParent, true >;
       addState(f);
       widgetOrder+= id;
       return ifigure(id ,fig1);
       }
       
IFigure _button(str id, Figure f, str txt, bool addSvgTag) {
       int width = f.width;
       int height = f.height; 
       str begintag = "";
       if (addSvgTag) {
          begintag+=
         "\<svg id=\"<id>_svg\"\> \<rect id=\"<id>\"/\> 
         '\<foreignObject id=\"<id>_fo\" x=0 y=0, width=\"<screenWidth>px\" height=\"<screenHeight>px\"\>";
         }
       begintag+="                    
            '\<button id=\"<id>\"\>
            "
            ;
       str endtag="
            '\</button\>
            "
            ;
       if (addSvgTag) {
            endtag += "\</foreignObject\>\</svg\>"; 
          }
        widget[id] = <getCallback(f.event), seq, id, begintag, endtag, 
        "    
        'd3.select(\"#<id>\") 
        '<on(getEvent(f.event), "doFunction(\"<id>\")")>
        '<stylePx("width", width)><stylePx("height", height)>   
        '<debugStyle()>
        '<style("background-color", "<getFillColor(f)>")> 
        '.text(\"<txt>\")   
        ;"
        , width, height, getAtX(f), getAtY(f), 0, 0, f.align, getLineWidth(f), getLineColor(f), f.sizeFromParent, false >;
       addState(f);
       widgetOrder+= id;
       return ifigure(id ,[]);
       }
       
set[IFigure] getUndefCells(list[IFigure] fig1) {
     return {q|q<-fig1,(getWidth(q)<0 || getHeight(q)<0)};
     }
     
int widthDefCells(list[IFigure] fig1) {
     if (isEmpty(fig1)) return 0;
     return sum([0]+[getWidth(q)|q<-fig1,getWidth(q)>=0]);
     }

int heightDefCells(list[IFigure] fig1) {
     if (isEmpty(fig1)) return 0;
     return sum([0]+[getHeight(q)|q<-fig1,getHeight(q)>=0]);
     }    
                
IFigure _hcat(str id, Figure f, bool addSvgTag, IFigure fig1...) {
       int width = f.width;
       int height = f.height; 
       str begintag = "";
       if (addSvgTag) {
          begintag+=
         "\<svg id=\"<id>_svg\"\> \<rect id=\"<id>_rect\"/\> 
         '\<foreignObject id=\"<id>_fo\" x=0 y=0, width=\"<screenWidth>px\" height=\"<screenHeight>px\"\>";
         }
       begintag+="                    
            '\<table id=\"<id>\" cellspacing=\"0\" cellpadding=\"0\"\>
            '\<tr\>"
            ;
       str endtag="
            '\</tr\>
            '\</table\>
            "
            ;
       if (addSvgTag) {
            endtag += "\</foreignObject\>\</svg\>"; 
            }
         //   '\<p\>\</p/\>
        widget[id] = <null, seq, id, begintag, endtag, 
        "
        'd3.select(\"#<id>\") 
        '<stylePx("width", width)><stylePx("height", height)>
        '<attrPx("width", width)><attrPx("height", height)>      
        '<debugStyle()>
        '<style("background-color", "<getFillColor(f)>")> 
        '<style("border-spacing", "<f.hgap> <f.vgap>")> 
        '<style("stroke-width",getLineWidth(f))>
        '<_padding(f.padding)>     
        ;   
        "
        , width, height, getAtX(f), getAtY(f), 0, 0, f.align, getLineWidth(f), getLineColor(f)
        , f.sizeFromParent, false >;
       addState(f);
       widgetOrder+= id;
       adjust+=  "adjustTableW(<[getId(c)|c<-fig1]>, \"<id>\", <getLineWidth(f)<0?0:-getLineWidth(f)>, 
               <-hPadding(f)>, <-vPadding(f)>);\n";
       return ifigure(id ,[td("<id>_<getId(g)>", f, g, width, height)| g<-fig1]);
       }
       
IFigure _vcat(str id, Figure f,  bool addSvgTag, IFigure fig1...) {
       int width = f.width;
       int height = f.height; 
       str begintag = "";
       if (addSvgTag) {
          begintag+=
         "\<svg id=\"<id>_svg\"\> x=0 y=0 \<rect id=\"<id>_rect\"/\> 
         '\<foreignObject id=\"<id>_fo\" x=0 y=0 width=\"<screenWidth>px\" height=\"<screenHeight>px\"\>";
         }
       begintag+="                 
            '\<table id=\"<id>\" cellspacing=\"0\" cellpadding=\"0\"\>"
           ;
       str endtag="\</table\>";
       if (addSvgTag) {
            endtag += "\</foreignObject\>\</svg\>"; 
            }
        widget[id] = <null, seq, id, begintag, endtag, 
        "
        'd3.select(\"#<id>\") 
        '<stylePx("width", width)><stylePx("height", height)> 
        '<attrPx("width", width)><attrPx("height", height)>       
        '<attr("fill",getFillColor(f))><attr("stroke",getLineColor(f))>
        '<style("border-spacing", "<f.hgap> <f.vgap>")>
        '<style("stroke-width",getLineWidth(f))>
        '<_padding(f.padding)> 
        ;
        ", width, height, getAtX(f), getAtY(f), 0, 0, f.align, getLineWidth(f), getLineColor(f)
         , f.sizeFromParent, false >;
       
       addState(f);
       widgetOrder+= id;
          adjust+=  "adjustTableH(<[getId(c)|c<-fig1]>, \"<id>\", <getLineWidth(f)<0?0:-getLineWidth(f)>, 
          <-hPadding(f)>, <-vPadding(f)>);\n";
       return ifigure(id, [td("<id>_<getId(g)>", f, g,  width, height, tr = true)| g<-fig1]);
       }
      
list[list[IFigure]] transpose(list[list[IFigure]] f) {
       list[list[IFigure]] r = [[]|i<-[0..max([size(d)|d<-f])]];
       for (int i<-[0..size(f)]) {
            for (int j<-[0..size(f[i])]) {
                r[j] = r[j] + f[i][j];
            }
         } 
       return r; 
       }

IFigure _grid(str id, Figure f,  bool addSvgTag, list[list[IFigure]] figArray=[[]]) {
       // if (f.lineWidth<0) f.lineWidth = 1;
       list[list[IFigure]] figArray1 = transpose(figArray);
       str begintag = "";
       if (addSvgTag) {
          begintag+=
         "\<svg id=\"<id>_svg\"\> \<rect id=\"<id>_rect\"/\> 
         '\<foreignObject id=\"<id>_fo\" x=0 y=0, width=\"<screenWidth>px\" height=\"<screenHeight>px\"\>";
         }
       begintag+="                    
            '\<table id=\"<id>\" cellspacing=\"0\" cellpadding=\"0\"\>
            ";
       str endtag="
            '\</table\>
            ";
       if (addSvgTag) {
            endtag += "\</foreignObject\>\</svg\>"; 
            }
        widget[id] = <null, seq, id, begintag, endtag, 
        "
        'd3.select(\"#<id>\")       
         '<debugStyle()>
         '<stylePx("width", f.width)><stylePx("height", f.height)> 
         '<attrPx("width", f.width)><attrPx("height", f.height)>   
         '<style("background-color", "<getFillColor(f)>")>
         '<style("border-spacing", "<f.hgap> <f.vgap>")>
         '<style("stroke-width",getLineWidth(f))>
         '<_padding(f.padding)> 
         '<debugStyle()>; 
        ", f.width, f.height, getAtX(f), getAtY(f), 0, 0, f.align, getLineWidth(f), getLineColor(f)
         , f.sizeFromParent, false >;      
       addState(f);
       widgetOrder+= id;
       list[tuple[list[IFigure] f, int idx]] fig1 = [<figArray[i], i>|int i<-[0..size(figArray)]];
       adjust+=  "adjustTableWH(<[[getId(d)|d<-c] | c<-figArray]>, \"<id>\", <-getLineWidth(f)>, 
          <-hPadding(f)>, <-vPadding(f)>);\n";
       return ifigure(id, [tr("<id>_<g.idx>", f, f.width, f.height, g.f ) | g<-fig1]);
       }      
   
 IFigure td(str id, Figure f, IFigure fig1, int width, int height, bool tr = false) {
    str begintag = tr?"\<tr\>":"";
    begintag +="\<td  id=\"<id>\" <vAlign(f.align)> <hAlign(f.align)>\>";   
    str endtag = "\</td\>";
    if (tr) endtag+="\</tr\>";
    widget[id] = <null, seq, id, begintag, endtag,
        "
        'd3.select(\"#<id>\")
        '<debug?debugStyle():borderStyle(f)>       
        '<style("background-color", "<getFillColor(f)>")>      
        ", f.width, f.height, getAtX(f), getAtY(f), 0, 0, f.align, getLineWidth(f), getLineColor(f)
         , f.sizeFromParent, false >;
       addState(f);
       widgetOrder+= id;
    return ifigure(id, [fig1]);
    }
    
 IFigure tr(str id, Figure f, int width, int height, list[IFigure] figs) {
    str begintag = "\<tr\>";
    str endtag="\</tr\>";
    widget[id] = <null, seq, id, begintag, endtag,
        "
        ", width, height, width, height, 0, 0, f.align, getLineWidth(f), getLineColor(f)
         , f.sizeFromParent, false >;
       addState(f);
       widgetOrder+= id;
       
       // if (width<0) adjust+= "adjust1(\"<id>\", \"<getId(fig)>\");\n";
    return ifigure(id, [td("<id>_<getId(g)>", f, g, width,  height
    )| g<-figs]);
    }
    
// str locArg(loc v) = isCursor(v) ? "{\"use\": <trCursor(v, deep = true)>}" : 
//					(v.scheme == "file" ? "\"<site>/<v.path>\"" : "\"<"<v>"[1..-1]>\"");
    
 IFigure _img(str id, Figure f, bool addSvgTag) {
       int width = f.width;
       int height = f.height; 
       str begintag = "";
       if (addSvgTag) {
          begintag+=
         "\<svg id=\"<id>_svg\"\> \<rect id=\"<id>_rect\"/\> 
         '\<foreignObject id=\"<id>_fo\" x=0 y=0, width=\"<screenWidth>px\" height=\"<screenHeight>px\"\>";
         }
       begintag+="                    
            '\<img id=\"<id>\" src = \"<f.src>\" alt = \"Not found:<f.src>\"\>
            "
            ;
       str endtag="
            '\</img\>
            "
            ;
       if (addSvgTag) {
            endtag += "\</foreignObject\>\</svg\>"; 
            }
         //   '\<p\>\</p/\>
        widget[id] = <null, seq, id, begintag, endtag, 
        "
        'd3.select(\"#<id>\") 
        '<stylePx("width", width)><stylePx("height", height)>
        '<attrPx("width", width)><attrPx("height", height)>      
        '<debugStyle()>
        '<style("background-color", "<getFillColor(f)>")> 
        '<style("border-spacing", "<f.hgap> <f.vgap>")> 
        '<style("stroke-width",getLineWidth(f))>
        '<_padding(f.padding)>     
        ;   
        "
        , width, height, getAtX(f), getAtY(f), 0, 0, f.align, getLineWidth(f), getLineColor(f)
        , f.sizeFromParent, false >;
       addState(f);
       widgetOrder+= id;
       return ifigure(id ,[]);
       }
    
bool isCentered(Figure f) = (ellipse():=f) || (circle():=f) || (ngon():=f);

list[tuple[str, Figure]] addMarkers(Figure f, list[tuple[str, Figure]] ms) {
    list[tuple[str, Figure]] r = [];
    for (<str id, Figure g><-ms) {
        if (emptyFigure()!:=g) g = addMarker(f, "<g.id>", g);
        r+=<id, g>;
        }
    return r;
    }



Figure addMarker(Figure f, str id, Figure g) {
   if (emptyFigure()!:=g) {
     if (g.size != <0, 0>) {
       g.width = g.size[0];
       g.height = g.size[1];
       }
    if (circle():=g || ngon():=g) {
      if (g.width<0) g.width = round(2 * g.r);
      if (g.height<0) g.height = round(2 * g.r);
      }
    if (ellipse():=g) {
      if (g.width<0) g.width = round(2 * g.rx);
      if (g.height<0) g.height = round(2 * g.ry);
      }
    if (g.gap != <0, 0>) {
       g.hgap = g.gap[0];
       g.vgap = g.gap[1];
       }
     g.id = "<id>";
     if (g.lineWidth<0) g.lineWidth = 1;
     if (g.lineOpacity<0) g.lineOpacity = 1.0;
     if (g.fillOpacity<0) g.fillOpacity = 1.0;
     if (isEmpty(g.fillColor)) g.fillColor="none";
     if (isEmpty(g.lineColor)) g.lineColor = "black";
     list[IFigure] fs = (defs[f.id]?)?defs[f.id]:[];
     buildParentTree(g);
     // println("addMarker:<g.id> <g.lineWidth>");
     figMap[g.id] = g;
     fs +=_translate(g, align = centerMid, fo = true);
         defs[f.id] = fs;
       }  
     // println("addMarker:  <g.id> <g.size>  <g.width>"); 
     return g;
   }
    
Alignment nA(Figure f, Figure fig) =  
     (f.width<0 || f.height<0 || getAtX(fig)>0 || getAtY(fig)>0)?(isCentered(f)?centerMid:topLeft):f.align;
     
Figure cL(Figure parent, Figure child) { 
    if (!isEmpty(child.id)) parentMap[child.id] = parent.id; 
    return child;
    }

Figure pL(Figure f) {
     if (isEmpty(f.id)) {
         f.id = "i<occur>";
         occur = occur + 1; 
         }
     figMap[f.id] = f;
     return f;
     }

Figure buildFigMap(Figure f) {  
    return visit(f) {
       case Figure g => pL(g)
    }  
   }
 

void buildParentTree(Figure f) {
    visit(f) {
        case g:box(): cL(g, g.fig);
        case g:frame():cL(g, g.fig);
        case g:ellipse():cL(g, g.fig);
        case g:circle():cL(g, g.fig);
        case g:ngon():{cL(g, g.fig);}
        case g:hcat(): for (q<-g.figs) cL(g, q);
        case g:vcat(): for (q<-g.figs) cL(g, q);
        case g:grid(): for (e<-g.figArray) for (q<-e) cL(g, q);
        case g:overlay(): for (q<-g.figs) cL(g, q); 
        case g:at(_, _, fg):  cL(g, fg); 
        case g:atX(_, fg):  cL(g, fg); 
        case g:atY(_, fg):  cL(g, fg);
        case g:graph():  for (q<-g.nodes) cL(g, q[1]);
        case g:rotate(_, fg):  cL(g, fg); 
        case g:rotate(_, _, _, fg):  cL(g, fg); 
        }  
        return; 
    } 
    
 
IFigure _translate(Figure f,  Alignment align = <0.5, 0.5>, bool addSvgTag = false,
    bool fo = true) {
    if (f.size != <0, 0>) {
       f.width = f.size[0];
       f.height = f.size[1];
       }
    if (f.gap != <0, 0>) {
       f.hgap = f.gap[0];
       f.vgap = f.gap[1];
       }
    if (f.cellWidth<0) f.cellWidth = f.width;
    if (f.cellHeight<0) f.cellHeight = f.height;
    switch(f) {
        case emptyFigure(): return iemptyFigure();
        case box():  return _rect(f.id, fo, f, fig = _translate(f.fig, align = nA(f, f.fig), fo = fo), align = align);
        case frame():  {
                   f.sizeFromParent = true;
                   f.lineWidth = 0; f.fillColor="none";
                   return _rect(f.id, fo, f, fig = _translate(f.fig, align = nA(f, f.fig), fo = fo), align = align
                       );
                   }
        case ellipse():  return _ellipse(f.id, fo, f, fig = 
             _translate(f.fig, align = nA(f, f.fig)), align = align 
             );
        case circle():  return _ellipse(f.id, fo, f, fig = _translate(f.fig, align = nA(f, f.fig), fo = fo), align = align);
        case polygon():  return _polygon(f.id,  f, align = align);
        case shape(list[Vertex] _):  {
                       addMarker(f, "<f.id>_start", f.startMarker);
                       addMarker(f, "<f.id>_mid", f.midMarker);
                       addMarker(f, "<f.id>_end", f.endMarker);
                       return _shape(f.id, f);
                       }
        case ngon():  return _ngon(f.id, fo, f, fig = _translate(f.fig, align = nA(f, f.fig), fo = fo), align = align);
        case text(value s): {if (str t:=s) return _text(f.id, fo, f, t);
                            return iemptyFigure();
                            } 
        case image():  return _img(f.id,   f, addSvgTag,  align = align);                
        case hcat(): return _hcat(f.id, f, addSvgTag, [_translate(q, align = f.align)|q<-f.figs]
            );
        case vcat(): return _vcat(f.id, f, addSvgTag, [_translate(q, align = f.align)|q<-f.figs]
         );
         
        case overlay(): { 
              IFigure r = _overlay(f.id, f, [_translate(q, addSvgTag = true)|q<-f.figs]);
              // return _rect("<f.id>_box", fo, f, fig = r, align = topLeft); 
              return r;
              }
              
        case grid(): return _grid(f.id, f, addSvgTag, figArray= [[_translate(q, align = f.align)|q<-e]|e<-f.figArray]
        );
        case at(int x, int y, Figure fig): {
                     fig.rotate = f.rotate;
                     fig.at = <x, y>; 
                     return _translate(fig, align = align, fo = fo);
                     }
        case atX(int x, Figure fig):	{
                    fig.rotate = f.rotate;
                    fig.at = <x, 0>; 
                    return _translate(fig, align = align, fo = fo);
                    }			
        case atY(int y, Figure fig):	{
                    fig.rotate = f.rotate;
                    fig.at = <0, y>; 
                    return _translate(fig, align = align, fo = fo);
                    }
        //case rotate(int angle, int x, int y, Figure fig): {
        //   // fig.at = f.at;
        //   fig.rotate = <angle, x, y>; return _translate(fig, align = align, fo = fo);}
        case rotate(int angle,  Figure fig): {
           // fig.at = f.at;
           fig.rotate = <angle, -1, -1>; 
           fig.at = f.at;
           return _translate(fig, align = align, fo = fo);
           }		
        case button(str txt):  return _button(f.id, f,  txt, addSvgTag);
        case combochart():  return _googlechart("ComboChart", f.id, f);
        case piechart():  return _googlechart("PieChart", f.id, f);
        case candlestickchart():  return _googlechart("CandlestickChart", f.id, f);
        case linechart():  return _googlechart("LineChart", f.id, f);
        case scatterchart():  return _googlechart("ScatterChart", f.id, f);
        case areachart():  return _googlechart("AreaChart", f.id, f);
        case graph(): {
              list[IFigure] ifs = [_translate(q, addSvgTag = true)|q<-[n[1]|n<-f.nodes]];
              IFigure r =
               _overlay("<f.id>_ov", f 
                , [_graph(f.id, f, [getId(i)|i<-ifs])]
                 + 
                ifs
                );
               return r;        
        }
       }
    }
    
    
public void _render(Figure fig1, int width = 400, int height = 400, 
     Alignment align = centerMid, tuple[int, int] size = <0, 0>,
     str fillColor = "white", str lineColor = "black", 
     int lineWidth = 1, bool display = true, num lineOpacity = 1.0, num fillOpacity = 1.0)
     {
        
        id = 0;
        screenHeight = height;
        screenWidth = width;
        if (size != <0, 0>) {
            screenWidth = size[0];
            screenHeight = size[1];
         }
        clearWidget();
        if (at(_, _, _):= fig1 || atX(_,_):=fig1 || atY(_,_):=fig1 
        // || fig1.height<0 || fig1.width<0 
        ){
             align = topLeft; 
             }  
        Figure h = emptyFigure();
        h.lineWidth = lineWidth;
        h.fillColor = fillColor;
        h.lineColor = lineColor;
        h.lineOpacity = lineOpacity;
        h.fillOpacity = fillOpacity;
        h.align = align;
        h.id = "figureArea";
        figMap[h.id] = h;
        fig1= buildFigMap(fig1);
        parentMap[fig1.id] = h.id; 
        buildParentTree(fig1);
        IFigure f = _translate(fig1, align = align);
        addState(fig1);
        _render(f , width = screenWidth, height = screenHeight, align = align, fillColor = fillColor, lineColor = lineColor,
        lineWidth = lineWidth, display = display);
     }
  
 //public void main() {
 //   clearWidget();
 //   IFigure fig0 = _rect("asbak",  emptyFigure(), fillColor = "antiquewhite", width = 50, height = 50, align = centerMid);
 //   _render(fig0);
 //   }
    