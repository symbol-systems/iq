import{as as j,c as it,s as ce,g as le,v as ue,x as de,b as fe,a as he,z as me,m as ke,l as pt,h as ht,i as ye,j as ge,y as pe}from"./aWg6KoXh.js";import{bf as St,bg as Et,c9 as xe,ca as ve,cb as be,cc as Te,cd as we,ce as Pt,cf as Nt,cg as Rt,ch as Bt,ci as Ht,cj as Gt,ck as Xt,cl as _e,cm as De,cn as Ce,co as Se,cp as Ee,cq as Me,cr as Ae}from"./CLgCPQXA.js";function Le(t){return t}var kt=1,bt=2,Tt=3,mt=4,jt=1e-6;function Ie(t){return"translate("+t+",0)"}function Ye(t){return"translate(0,"+t+")"}function Fe(t){return e=>+t(e)}function We(t,e){return e=Math.max(0,t.bandwidth()-e*2)/2,t.round()&&(e=Math.round(e)),s=>+t(s)+e}function Ve(){return!this.__axis}function Zt(t,e){var s=[],r=null,a=null,h=6,f=6,T=3,M=typeof window<"u"&&window.devicePixelRatio>1?0:.5,g=t===kt||t===mt?-1:1,D=t===mt||t===bt?"x":"y",S=t===kt||t===Tt?Ie:Ye;function _(v){var H=r??(e.ticks?e.ticks.apply(e,s):e.domain()),m=a??(e.tickFormat?e.tickFormat.apply(e,s):Le),C=Math.max(h,0)+T,I=e.range(),L=+I[0]+M,O=+I[I.length-1]+M,P=(e.bandwidth?We:Fe)(e.copy(),M),G=v.selection?v.selection():v,N=G.selectAll(".domain").data([null]),z=G.selectAll(".tick").data(H,e).order(),k=z.exit(),w=z.enter().append("g").attr("class","tick"),p=z.select("line"),y=z.select("text");N=N.merge(N.enter().insert("path",".tick").attr("class","domain").attr("stroke","currentColor")),z=z.merge(w),p=p.merge(w.append("line").attr("stroke","currentColor").attr(D+"2",g*h)),y=y.merge(w.append("text").attr("fill","currentColor").attr(D,g*C).attr("dy",t===kt?"0em":t===Tt?"0.71em":"0.32em")),v!==G&&(N=N.transition(v),z=z.transition(v),p=p.transition(v),y=y.transition(v),k=k.transition(v).attr("opacity",jt).attr("transform",function(n){return isFinite(n=P(n))?S(n+M):this.getAttribute("transform")}),w.attr("opacity",jt).attr("transform",function(n){var u=this.parentNode.__axis;return S((u&&isFinite(u=u(n))?u:P(n))+M)})),k.remove(),N.attr("d",t===mt||t===bt?f?"M"+g*f+","+L+"H"+M+"V"+O+"H"+g*f:"M"+M+","+L+"V"+O:f?"M"+L+","+g*f+"V"+M+"H"+O+"V"+g*f:"M"+L+","+M+"H"+O),z.attr("opacity",1).attr("transform",function(n){return S(P(n)+M)}),p.attr(D+"2",g*h),y.attr(D,g*C).text(m),G.filter(Ve).attr("fill","none").attr("font-size",10).attr("font-family","sans-serif").attr("text-anchor",t===bt?"start":t===mt?"end":"middle"),G.each(function(){this.__axis=P})}return _.scale=function(v){return arguments.length?(e=v,_):e},_.ticks=function(){return s=Array.from(arguments),_},_.tickArguments=function(v){return arguments.length?(s=v==null?[]:Array.from(v),_):s.slice()},_.tickValues=function(v){return arguments.length?(r=v==null?null:Array.from(v),_):r&&r.slice()},_.tickFormat=function(v){return arguments.length?(a=v,_):a},_.tickSize=function(v){return arguments.length?(h=f=+v,_):h},_.tickSizeInner=function(v){return arguments.length?(h=+v,_):h},_.tickSizeOuter=function(v){return arguments.length?(f=+v,_):f},_.tickPadding=function(v){return arguments.length?(T=+v,_):T},_.offset=function(v){return arguments.length?(M=+v,_):M},_}function ze(t){return Zt(kt,t)}function Oe(t){return Zt(Tt,t)}var Qt={exports:{}};(function(t,e){(function(s,r){t.exports=r()})(St,function(){var s="day";return function(r,a,h){var f=function(g){return g.add(4-g.isoWeekday(),s)},T=a.prototype;T.isoWeekYear=function(){return f(this).year()},T.isoWeek=function(g){if(!this.$utils().u(g))return this.add(7*(g-this.isoWeek()),s);var D,S,_,v,H=f(this),m=(D=this.isoWeekYear(),S=this.$u,_=(S?h.utc:h)().year(D).startOf("year"),v=4-_.isoWeekday(),_.isoWeekday()>4&&(v+=7),_.add(v,s));return H.diff(m,"week")+1},T.isoWeekday=function(g){return this.$utils().u(g)?this.day()||7:this.day(this.day()%7?g:g-7)};var M=T.startOf;T.startOf=function(g,D){var S=this.$utils(),_=!!S.u(D)||D;return S.p(g)==="isoweek"?_?this.date(this.date()-(this.isoWeekday()-1)).startOf("day"):this.date(this.date()-1-(this.isoWeekday()-1)+7).endOf("day"):M.bind(this)(g,D)}}})})(Qt);var Pe=Qt.exports;const Ne=Et(Pe);var Jt={exports:{}};(function(t,e){(function(s,r){t.exports=r()})(St,function(){var s={LTS:"h:mm:ss A",LT:"h:mm A",L:"MM/DD/YYYY",LL:"MMMM D, YYYY",LLL:"MMMM D, YYYY h:mm A",LLLL:"dddd, MMMM D, YYYY h:mm A"},r=/(\[[^[]*\])|([-_:/.,()\s]+)|(A|a|YYYY|YY?|MM?M?M?|Do|DD?|hh?|HH?|mm?|ss?|S{1,3}|z|ZZ?)/g,a=/\d\d/,h=/\d\d?/,f=/\d*[^-_:/,()\s\d]+/,T={},M=function(m){return(m=+m)+(m>68?1900:2e3)},g=function(m){return function(C){this[m]=+C}},D=[/[+-]\d\d:?(\d\d)?|Z/,function(m){(this.zone||(this.zone={})).offset=function(C){if(!C||C==="Z")return 0;var I=C.match(/([+-]|\d\d)/g),L=60*I[1]+(+I[2]||0);return L===0?0:I[0]==="+"?-L:L}(m)}],S=function(m){var C=T[m];return C&&(C.indexOf?C:C.s.concat(C.f))},_=function(m,C){var I,L=T.meridiem;if(L){for(var O=1;O<=24;O+=1)if(m.indexOf(L(O,0,C))>-1){I=O>12;break}}else I=m===(C?"pm":"PM");return I},v={A:[f,function(m){this.afternoon=_(m,!1)}],a:[f,function(m){this.afternoon=_(m,!0)}],S:[/\d/,function(m){this.milliseconds=100*+m}],SS:[a,function(m){this.milliseconds=10*+m}],SSS:[/\d{3}/,function(m){this.milliseconds=+m}],s:[h,g("seconds")],ss:[h,g("seconds")],m:[h,g("minutes")],mm:[h,g("minutes")],H:[h,g("hours")],h:[h,g("hours")],HH:[h,g("hours")],hh:[h,g("hours")],D:[h,g("day")],DD:[a,g("day")],Do:[f,function(m){var C=T.ordinal,I=m.match(/\d+/);if(this.day=I[0],C)for(var L=1;L<=31;L+=1)C(L).replace(/\[|\]/g,"")===m&&(this.day=L)}],M:[h,g("month")],MM:[a,g("month")],MMM:[f,function(m){var C=S("months"),I=(S("monthsShort")||C.map(function(L){return L.slice(0,3)})).indexOf(m)+1;if(I<1)throw new Error;this.month=I%12||I}],MMMM:[f,function(m){var C=S("months").indexOf(m)+1;if(C<1)throw new Error;this.month=C%12||C}],Y:[/[+-]?\d+/,g("year")],YY:[a,function(m){this.year=M(m)}],YYYY:[/\d{4}/,g("year")],Z:D,ZZ:D};function H(m){var C,I;C=m,I=T&&T.formats;for(var L=(m=C.replace(/(\[[^\]]+])|(LTS?|l{1,4}|L{1,4})/g,function(w,p,y){var n=y&&y.toUpperCase();return p||I[y]||s[y]||I[n].replace(/(\[[^\]]+])|(MMMM|MM|DD|dddd)/g,function(u,d,o){return d||o.slice(1)})})).match(r),O=L.length,P=0;P<O;P+=1){var G=L[P],N=v[G],z=N&&N[0],k=N&&N[1];L[P]=k?{regex:z,parser:k}:G.replace(/^\[|\]$/g,"")}return function(w){for(var p={},y=0,n=0;y<O;y+=1){var u=L[y];if(typeof u=="string")n+=u.length;else{var d=u.regex,o=u.parser,x=w.slice(n),i=d.exec(x)[0];o.call(p,i),w=w.replace(i,"")}}return function(W){var c=W.afternoon;if(c!==void 0){var l=W.hours;c?l<12&&(W.hours+=12):l===12&&(W.hours=0),delete W.afternoon}}(p),p}}return function(m,C,I){I.p.customParseFormat=!0,m&&m.parseTwoDigitYear&&(M=m.parseTwoDigitYear);var L=C.prototype,O=L.parse;L.parse=function(P){var G=P.date,N=P.utc,z=P.args;this.$u=N;var k=z[1];if(typeof k=="string"){var w=z[2]===!0,p=z[3]===!0,y=w||p,n=z[2];p&&(n=z[2]),T=this.$locale(),!w&&n&&(T=I.Ls[n]),this.$d=function(x,i,W){try{if(["x","X"].indexOf(i)>-1)return new Date((i==="X"?1e3:1)*x);var c=H(i)(x),l=c.year,b=c.month,V=c.day,A=c.hours,Y=c.minutes,E=c.seconds,F=c.milliseconds,tt=c.zone,Q=new Date,at=V||(l||b?1:Q.getDate()),ot=l||Q.getFullYear(),R=0;l&&!b||(R=b>0?b-1:Q.getMonth());var q=A||0,X=Y||0,et=E||0,U=F||0;return tt?new Date(Date.UTC(ot,R,at,q,X,et,U+60*tt.offset*1e3)):W?new Date(Date.UTC(ot,R,at,q,X,et,U)):new Date(ot,R,at,q,X,et,U)}catch{return new Date("")}}(G,k,N),this.init(),n&&n!==!0&&(this.$L=this.locale(n).$L),y&&G!=this.format(k)&&(this.$d=new Date("")),T={}}else if(k instanceof Array)for(var u=k.length,d=1;d<=u;d+=1){z[1]=k[d-1];var o=I.apply(this,z);if(o.isValid()){this.$d=o.$d,this.$L=o.$L,this.init();break}d===u&&(this.$d=new Date(""))}else O.call(this,P)}}})})(Jt);var Re=Jt.exports;const Be=Et(Re);var Kt={exports:{}};(function(t,e){(function(s,r){t.exports=r()})(St,function(){return function(s,r){var a=r.prototype,h=a.format;a.format=function(f){var T=this,M=this.$locale();if(!this.isValid())return h.bind(this)(f);var g=this.$utils(),D=(f||"YYYY-MM-DDTHH:mm:ssZ").replace(/\[([^\]]+)]|Q|wo|ww|w|WW|W|zzz|z|gggg|GGGG|Do|X|x|k{1,2}|S/g,function(S){switch(S){case"Q":return Math.ceil((T.$M+1)/3);case"Do":return M.ordinal(T.$D);case"gggg":return T.weekYear();case"GGGG":return T.isoWeekYear();case"wo":return M.ordinal(T.week(),"W");case"w":case"ww":return g.s(T.week(),S==="w"?1:2,"0");case"W":case"WW":return g.s(T.isoWeek(),S==="W"?1:2,"0");case"k":case"kk":return g.s(String(T.$H===0?24:T.$H),S==="k"?1:2,"0");case"X":return Math.floor(T.$d.getTime()/1e3);case"x":return T.$d.getTime();case"z":return"["+T.offsetName()+"]";case"zzz":return"["+T.offsetName("long")+"]";default:return S}});return h.bind(this)(D)}}})})(Kt);var He=Kt.exports;const Ge=Et(He);var wt=function(){var t=function(y,n,u,d){for(u=u||{},d=y.length;d--;u[y[d]]=n);return u},e=[6,8,10,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,30,32,33,35,37],s=[1,25],r=[1,26],a=[1,27],h=[1,28],f=[1,29],T=[1,30],M=[1,31],g=[1,9],D=[1,10],S=[1,11],_=[1,12],v=[1,13],H=[1,14],m=[1,15],C=[1,16],I=[1,18],L=[1,19],O=[1,20],P=[1,21],G=[1,22],N=[1,24],z=[1,32],k={trace:function(){},yy:{},symbols_:{error:2,start:3,gantt:4,document:5,EOF:6,line:7,SPACE:8,statement:9,NL:10,weekday:11,weekday_monday:12,weekday_tuesday:13,weekday_wednesday:14,weekday_thursday:15,weekday_friday:16,weekday_saturday:17,weekday_sunday:18,dateFormat:19,inclusiveEndDates:20,topAxis:21,axisFormat:22,tickInterval:23,excludes:24,includes:25,todayMarker:26,title:27,acc_title:28,acc_title_value:29,acc_descr:30,acc_descr_value:31,acc_descr_multiline_value:32,section:33,clickStatement:34,taskTxt:35,taskData:36,click:37,callbackname:38,callbackargs:39,href:40,clickStatementDebug:41,$accept:0,$end:1},terminals_:{2:"error",4:"gantt",6:"EOF",8:"SPACE",10:"NL",12:"weekday_monday",13:"weekday_tuesday",14:"weekday_wednesday",15:"weekday_thursday",16:"weekday_friday",17:"weekday_saturday",18:"weekday_sunday",19:"dateFormat",20:"inclusiveEndDates",21:"topAxis",22:"axisFormat",23:"tickInterval",24:"excludes",25:"includes",26:"todayMarker",27:"title",28:"acc_title",29:"acc_title_value",30:"acc_descr",31:"acc_descr_value",32:"acc_descr_multiline_value",33:"section",35:"taskTxt",36:"taskData",37:"click",38:"callbackname",39:"callbackargs",40:"href"},productions_:[0,[3,3],[5,0],[5,2],[7,2],[7,1],[7,1],[7,1],[11,1],[11,1],[11,1],[11,1],[11,1],[11,1],[11,1],[9,1],[9,1],[9,1],[9,1],[9,1],[9,1],[9,1],[9,1],[9,1],[9,1],[9,2],[9,2],[9,1],[9,1],[9,1],[9,2],[34,2],[34,3],[34,3],[34,4],[34,3],[34,4],[34,2],[41,2],[41,3],[41,3],[41,4],[41,3],[41,4],[41,2]],performAction:function(n,u,d,o,x,i,W){var c=i.length-1;switch(x){case 1:return i[c-1];case 2:this.$=[];break;case 3:i[c-1].push(i[c]),this.$=i[c-1];break;case 4:case 5:this.$=i[c];break;case 6:case 7:this.$=[];break;case 8:o.setWeekday("monday");break;case 9:o.setWeekday("tuesday");break;case 10:o.setWeekday("wednesday");break;case 11:o.setWeekday("thursday");break;case 12:o.setWeekday("friday");break;case 13:o.setWeekday("saturday");break;case 14:o.setWeekday("sunday");break;case 15:o.setDateFormat(i[c].substr(11)),this.$=i[c].substr(11);break;case 16:o.enableInclusiveEndDates(),this.$=i[c].substr(18);break;case 17:o.TopAxis(),this.$=i[c].substr(8);break;case 18:o.setAxisFormat(i[c].substr(11)),this.$=i[c].substr(11);break;case 19:o.setTickInterval(i[c].substr(13)),this.$=i[c].substr(13);break;case 20:o.setExcludes(i[c].substr(9)),this.$=i[c].substr(9);break;case 21:o.setIncludes(i[c].substr(9)),this.$=i[c].substr(9);break;case 22:o.setTodayMarker(i[c].substr(12)),this.$=i[c].substr(12);break;case 24:o.setDiagramTitle(i[c].substr(6)),this.$=i[c].substr(6);break;case 25:this.$=i[c].trim(),o.setAccTitle(this.$);break;case 26:case 27:this.$=i[c].trim(),o.setAccDescription(this.$);break;case 28:o.addSection(i[c].substr(8)),this.$=i[c].substr(8);break;case 30:o.addTask(i[c-1],i[c]),this.$="task";break;case 31:this.$=i[c-1],o.setClickEvent(i[c-1],i[c],null);break;case 32:this.$=i[c-2],o.setClickEvent(i[c-2],i[c-1],i[c]);break;case 33:this.$=i[c-2],o.setClickEvent(i[c-2],i[c-1],null),o.setLink(i[c-2],i[c]);break;case 34:this.$=i[c-3],o.setClickEvent(i[c-3],i[c-2],i[c-1]),o.setLink(i[c-3],i[c]);break;case 35:this.$=i[c-2],o.setClickEvent(i[c-2],i[c],null),o.setLink(i[c-2],i[c-1]);break;case 36:this.$=i[c-3],o.setClickEvent(i[c-3],i[c-1],i[c]),o.setLink(i[c-3],i[c-2]);break;case 37:this.$=i[c-1],o.setLink(i[c-1],i[c]);break;case 38:case 44:this.$=i[c-1]+" "+i[c];break;case 39:case 40:case 42:this.$=i[c-2]+" "+i[c-1]+" "+i[c];break;case 41:case 43:this.$=i[c-3]+" "+i[c-2]+" "+i[c-1]+" "+i[c];break}},table:[{3:1,4:[1,2]},{1:[3]},t(e,[2,2],{5:3}),{6:[1,4],7:5,8:[1,6],9:7,10:[1,8],11:17,12:s,13:r,14:a,15:h,16:f,17:T,18:M,19:g,20:D,21:S,22:_,23:v,24:H,25:m,26:C,27:I,28:L,30:O,32:P,33:G,34:23,35:N,37:z},t(e,[2,7],{1:[2,1]}),t(e,[2,3]),{9:33,11:17,12:s,13:r,14:a,15:h,16:f,17:T,18:M,19:g,20:D,21:S,22:_,23:v,24:H,25:m,26:C,27:I,28:L,30:O,32:P,33:G,34:23,35:N,37:z},t(e,[2,5]),t(e,[2,6]),t(e,[2,15]),t(e,[2,16]),t(e,[2,17]),t(e,[2,18]),t(e,[2,19]),t(e,[2,20]),t(e,[2,21]),t(e,[2,22]),t(e,[2,23]),t(e,[2,24]),{29:[1,34]},{31:[1,35]},t(e,[2,27]),t(e,[2,28]),t(e,[2,29]),{36:[1,36]},t(e,[2,8]),t(e,[2,9]),t(e,[2,10]),t(e,[2,11]),t(e,[2,12]),t(e,[2,13]),t(e,[2,14]),{38:[1,37],40:[1,38]},t(e,[2,4]),t(e,[2,25]),t(e,[2,26]),t(e,[2,30]),t(e,[2,31],{39:[1,39],40:[1,40]}),t(e,[2,37],{38:[1,41]}),t(e,[2,32],{40:[1,42]}),t(e,[2,33]),t(e,[2,35],{39:[1,43]}),t(e,[2,34]),t(e,[2,36])],defaultActions:{},parseError:function(n,u){if(u.recoverable)this.trace(n);else{var d=new Error(n);throw d.hash=u,d}},parse:function(n){var u=this,d=[0],o=[],x=[null],i=[],W=this.table,c="",l=0,b=0,V=2,A=1,Y=i.slice.call(arguments,1),E=Object.create(this.lexer),F={yy:{}};for(var tt in this.yy)Object.prototype.hasOwnProperty.call(this.yy,tt)&&(F.yy[tt]=this.yy[tt]);E.setInput(n,F.yy),F.yy.lexer=E,F.yy.parser=this,typeof E.yylloc>"u"&&(E.yylloc={});var Q=E.yylloc;i.push(Q);var at=E.options&&E.options.ranges;typeof F.yy.parseError=="function"?this.parseError=F.yy.parseError:this.parseError=Object.getPrototypeOf(this).parseError;function ot(){var K;return K=o.pop()||E.lex()||A,typeof K!="number"&&(K instanceof Array&&(o=K,K=o.pop()),K=u.symbols_[K]||K),K}for(var R,q,X,et,U={},ct,J,Ot,ft;;){if(q=d[d.length-1],this.defaultActions[q]?X=this.defaultActions[q]:((R===null||typeof R>"u")&&(R=ot()),X=W[q]&&W[q][R]),typeof X>"u"||!X.length||!X[0]){var vt="";ft=[];for(ct in W[q])this.terminals_[ct]&&ct>V&&ft.push("'"+this.terminals_[ct]+"'");E.showPosition?vt="Parse error on line "+(l+1)+`:
`+E.showPosition()+`
Expecting `+ft.join(", ")+", got '"+(this.terminals_[R]||R)+"'":vt="Parse error on line "+(l+1)+": Unexpected "+(R==A?"end of input":"'"+(this.terminals_[R]||R)+"'"),this.parseError(vt,{text:E.match,token:this.terminals_[R]||R,line:E.yylineno,loc:Q,expected:ft})}if(X[0]instanceof Array&&X.length>1)throw new Error("Parse Error: multiple actions possible at state: "+q+", token: "+R);switch(X[0]){case 1:d.push(R),x.push(E.yytext),i.push(E.yylloc),d.push(X[1]),R=null,b=E.yyleng,c=E.yytext,l=E.yylineno,Q=E.yylloc;break;case 2:if(J=this.productions_[X[1]][1],U.$=x[x.length-J],U._$={first_line:i[i.length-(J||1)].first_line,last_line:i[i.length-1].last_line,first_column:i[i.length-(J||1)].first_column,last_column:i[i.length-1].last_column},at&&(U._$.range=[i[i.length-(J||1)].range[0],i[i.length-1].range[1]]),et=this.performAction.apply(U,[c,b,l,F.yy,X[1],x,i].concat(Y)),typeof et<"u")return et;J&&(d=d.slice(0,-1*J*2),x=x.slice(0,-1*J),i=i.slice(0,-1*J)),d.push(this.productions_[X[1]][0]),x.push(U.$),i.push(U._$),Ot=W[d[d.length-2]][d[d.length-1]],d.push(Ot);break;case 3:return!0}}return!0}},w=function(){var y={EOF:1,parseError:function(u,d){if(this.yy.parser)this.yy.parser.parseError(u,d);else throw new Error(u)},setInput:function(n,u){return this.yy=u||this.yy||{},this._input=n,this._more=this._backtrack=this.done=!1,this.yylineno=this.yyleng=0,this.yytext=this.matched=this.match="",this.conditionStack=["INITIAL"],this.yylloc={first_line:1,first_column:0,last_line:1,last_column:0},this.options.ranges&&(this.yylloc.range=[0,0]),this.offset=0,this},input:function(){var n=this._input[0];this.yytext+=n,this.yyleng++,this.offset++,this.match+=n,this.matched+=n;var u=n.match(/(?:\r\n?|\n).*/g);return u?(this.yylineno++,this.yylloc.last_line++):this.yylloc.last_column++,this.options.ranges&&this.yylloc.range[1]++,this._input=this._input.slice(1),n},unput:function(n){var u=n.length,d=n.split(/(?:\r\n?|\n)/g);this._input=n+this._input,this.yytext=this.yytext.substr(0,this.yytext.length-u),this.offset-=u;var o=this.match.split(/(?:\r\n?|\n)/g);this.match=this.match.substr(0,this.match.length-1),this.matched=this.matched.substr(0,this.matched.length-1),d.length-1&&(this.yylineno-=d.length-1);var x=this.yylloc.range;return this.yylloc={first_line:this.yylloc.first_line,last_line:this.yylineno+1,first_column:this.yylloc.first_column,last_column:d?(d.length===o.length?this.yylloc.first_column:0)+o[o.length-d.length].length-d[0].length:this.yylloc.first_column-u},this.options.ranges&&(this.yylloc.range=[x[0],x[0]+this.yyleng-u]),this.yyleng=this.yytext.length,this},more:function(){return this._more=!0,this},reject:function(){if(this.options.backtrack_lexer)this._backtrack=!0;else return this.parseError("Lexical error on line "+(this.yylineno+1)+`. You can only invoke reject() in the lexer when the lexer is of the backtracking persuasion (options.backtrack_lexer = true).
`+this.showPosition(),{text:"",token:null,line:this.yylineno});return this},less:function(n){this.unput(this.match.slice(n))},pastInput:function(){var n=this.matched.substr(0,this.matched.length-this.match.length);return(n.length>20?"...":"")+n.substr(-20).replace(/\n/g,"")},upcomingInput:function(){var n=this.match;return n.length<20&&(n+=this._input.substr(0,20-n.length)),(n.substr(0,20)+(n.length>20?"...":"")).replace(/\n/g,"")},showPosition:function(){var n=this.pastInput(),u=new Array(n.length+1).join("-");return n+this.upcomingInput()+`
`+u+"^"},test_match:function(n,u){var d,o,x;if(this.options.backtrack_lexer&&(x={yylineno:this.yylineno,yylloc:{first_line:this.yylloc.first_line,last_line:this.last_line,first_column:this.yylloc.first_column,last_column:this.yylloc.last_column},yytext:this.yytext,match:this.match,matches:this.matches,matched:this.matched,yyleng:this.yyleng,offset:this.offset,_more:this._more,_input:this._input,yy:this.yy,conditionStack:this.conditionStack.slice(0),done:this.done},this.options.ranges&&(x.yylloc.range=this.yylloc.range.slice(0))),o=n[0].match(/(?:\r\n?|\n).*/g),o&&(this.yylineno+=o.length),this.yylloc={first_line:this.yylloc.last_line,last_line:this.yylineno+1,first_column:this.yylloc.last_column,last_column:o?o[o.length-1].length-o[o.length-1].match(/\r?\n?/)[0].length:this.yylloc.last_column+n[0].length},this.yytext+=n[0],this.match+=n[0],this.matches=n,this.yyleng=this.yytext.length,this.options.ranges&&(this.yylloc.range=[this.offset,this.offset+=this.yyleng]),this._more=!1,this._backtrack=!1,this._input=this._input.slice(n[0].length),this.matched+=n[0],d=this.performAction.call(this,this.yy,this,u,this.conditionStack[this.conditionStack.length-1]),this.done&&this._input&&(this.done=!1),d)return d;if(this._backtrack){for(var i in x)this[i]=x[i];return!1}return!1},next:function(){if(this.done)return this.EOF;this._input||(this.done=!0);var n,u,d,o;this._more||(this.yytext="",this.match="");for(var x=this._currentRules(),i=0;i<x.length;i++)if(d=this._input.match(this.rules[x[i]]),d&&(!u||d[0].length>u[0].length)){if(u=d,o=i,this.options.backtrack_lexer){if(n=this.test_match(d,x[i]),n!==!1)return n;if(this._backtrack){u=!1;continue}else return!1}else if(!this.options.flex)break}return u?(n=this.test_match(u,x[o]),n!==!1?n:!1):this._input===""?this.EOF:this.parseError("Lexical error on line "+(this.yylineno+1)+`. Unrecognized text.
`+this.showPosition(),{text:"",token:null,line:this.yylineno})},lex:function(){var u=this.next();return u||this.lex()},begin:function(u){this.conditionStack.push(u)},popState:function(){var u=this.conditionStack.length-1;return u>0?this.conditionStack.pop():this.conditionStack[0]},_currentRules:function(){return this.conditionStack.length&&this.conditionStack[this.conditionStack.length-1]?this.conditions[this.conditionStack[this.conditionStack.length-1]].rules:this.conditions.INITIAL.rules},topState:function(u){return u=this.conditionStack.length-1-Math.abs(u||0),u>=0?this.conditionStack[u]:"INITIAL"},pushState:function(u){this.begin(u)},stateStackSize:function(){return this.conditionStack.length},options:{"case-insensitive":!0},performAction:function(u,d,o,x){switch(o){case 0:return this.begin("open_directive"),"open_directive";case 1:return this.begin("acc_title"),28;case 2:return this.popState(),"acc_title_value";case 3:return this.begin("acc_descr"),30;case 4:return this.popState(),"acc_descr_value";case 5:this.begin("acc_descr_multiline");break;case 6:this.popState();break;case 7:return"acc_descr_multiline_value";case 8:break;case 9:break;case 10:break;case 11:return 10;case 12:break;case 13:break;case 14:this.begin("href");break;case 15:this.popState();break;case 16:return 40;case 17:this.begin("callbackname");break;case 18:this.popState();break;case 19:this.popState(),this.begin("callbackargs");break;case 20:return 38;case 21:this.popState();break;case 22:return 39;case 23:this.begin("click");break;case 24:this.popState();break;case 25:return 37;case 26:return 4;case 27:return 19;case 28:return 20;case 29:return 21;case 30:return 22;case 31:return 23;case 32:return 25;case 33:return 24;case 34:return 26;case 35:return 12;case 36:return 13;case 37:return 14;case 38:return 15;case 39:return 16;case 40:return 17;case 41:return 18;case 42:return"date";case 43:return 27;case 44:return"accDescription";case 45:return 33;case 46:return 35;case 47:return 36;case 48:return":";case 49:return 6;case 50:return"INVALID"}},rules:[/^(?:%%\{)/i,/^(?:accTitle\s*:\s*)/i,/^(?:(?!\n||)*[^\n]*)/i,/^(?:accDescr\s*:\s*)/i,/^(?:(?!\n||)*[^\n]*)/i,/^(?:accDescr\s*\{\s*)/i,/^(?:[\}])/i,/^(?:[^\}]*)/i,/^(?:%%(?!\{)*[^\n]*)/i,/^(?:[^\}]%%*[^\n]*)/i,/^(?:%%*[^\n]*[\n]*)/i,/^(?:[\n]+)/i,/^(?:\s+)/i,/^(?:%[^\n]*)/i,/^(?:href[\s]+["])/i,/^(?:["])/i,/^(?:[^"]*)/i,/^(?:call[\s]+)/i,/^(?:\([\s]*\))/i,/^(?:\()/i,/^(?:[^(]*)/i,/^(?:\))/i,/^(?:[^)]*)/i,/^(?:click[\s]+)/i,/^(?:[\s\n])/i,/^(?:[^\s\n]*)/i,/^(?:gantt\b)/i,/^(?:dateFormat\s[^#\n;]+)/i,/^(?:inclusiveEndDates\b)/i,/^(?:topAxis\b)/i,/^(?:axisFormat\s[^#\n;]+)/i,/^(?:tickInterval\s[^#\n;]+)/i,/^(?:includes\s[^#\n;]+)/i,/^(?:excludes\s[^#\n;]+)/i,/^(?:todayMarker\s[^\n;]+)/i,/^(?:weekday\s+monday\b)/i,/^(?:weekday\s+tuesday\b)/i,/^(?:weekday\s+wednesday\b)/i,/^(?:weekday\s+thursday\b)/i,/^(?:weekday\s+friday\b)/i,/^(?:weekday\s+saturday\b)/i,/^(?:weekday\s+sunday\b)/i,/^(?:\d\d\d\d-\d\d-\d\d\b)/i,/^(?:title\s[^\n]+)/i,/^(?:accDescription\s[^#\n;]+)/i,/^(?:section\s[^\n]+)/i,/^(?:[^:\n]+)/i,/^(?::[^#\n;]+)/i,/^(?::)/i,/^(?:$)/i,/^(?:.)/i],conditions:{acc_descr_multiline:{rules:[6,7],inclusive:!1},acc_descr:{rules:[4],inclusive:!1},acc_title:{rules:[2],inclusive:!1},callbackargs:{rules:[21,22],inclusive:!1},callbackname:{rules:[18,19,20],inclusive:!1},href:{rules:[15,16],inclusive:!1},click:{rules:[24,25],inclusive:!1},INITIAL:{rules:[0,1,3,5,8,9,10,11,12,13,14,17,23,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50],inclusive:!0}}};return y}();k.lexer=w;function p(){this.yy={}}return p.prototype=k,k.Parser=p,new p}();wt.parser=wt;const Xe=wt;j.extend(Ne);j.extend(Be);j.extend(Ge);let Z="",Mt="",At,Lt="",lt=[],ut=[],It={},Yt=[],xt=[],rt="",Ft="";const $t=["active","done","crit","milestone"];let Wt=[],dt=!1,Vt=!1,zt="sunday",_t=0;const je=function(){Yt=[],xt=[],rt="",Wt=[],yt=0,Ct=void 0,gt=void 0,B=[],Z="",Mt="",Ft="",At=void 0,Lt="",lt=[],ut=[],dt=!1,Vt=!1,_t=0,It={},me(),zt="sunday"},qe=function(t){Mt=t},Ue=function(){return Mt},Ze=function(t){At=t},Qe=function(){return At},Je=function(t){Lt=t},Ke=function(){return Lt},$e=function(t){Z=t},tn=function(){dt=!0},en=function(){return dt},nn=function(){Vt=!0},sn=function(){return Vt},rn=function(t){Ft=t},an=function(){return Ft},on=function(){return Z},cn=function(t){lt=t.toLowerCase().split(/[\s,]+/)},ln=function(){return lt},un=function(t){ut=t.toLowerCase().split(/[\s,]+/)},dn=function(){return ut},fn=function(){return It},hn=function(t){rt=t,Yt.push(t)},mn=function(){return Yt},kn=function(){let t=qt();const e=10;let s=0;for(;!t&&s<e;)t=qt(),s++;return xt=B,xt},te=function(t,e,s,r){return r.includes(t.format(e.trim()))?!1:t.isoWeekday()>=6&&s.includes("weekends")||s.includes(t.format("dddd").toLowerCase())?!0:s.includes(t.format(e.trim()))},yn=function(t){zt=t},gn=function(){return zt},ee=function(t,e,s,r){if(!s.length||t.manualEndTime)return;let a;t.startTime instanceof Date?a=j(t.startTime):a=j(t.startTime,e,!0),a=a.add(1,"d");let h;t.endTime instanceof Date?h=j(t.endTime):h=j(t.endTime,e,!0);const[f,T]=pn(a,h,e,s,r);t.endTime=f.toDate(),t.renderEndTime=T},pn=function(t,e,s,r,a){let h=!1,f=null;for(;t<=e;)h||(f=e.toDate()),h=te(t,s,r,a),h&&(e=e.add(1,"d")),t=t.add(1,"d");return[e,f]},Dt=function(t,e,s){s=s.trim();const a=/^after\s+(?<ids>[\d\w- ]+)/.exec(s);if(a!==null){let f=null;for(const M of a.groups.ids.split(" ")){let g=nt(M);g!==void 0&&(!f||g.endTime>f.endTime)&&(f=g)}if(f)return f.endTime;const T=new Date;return T.setHours(0,0,0,0),T}let h=j(s,e.trim(),!0);if(h.isValid())return h.toDate();{pt.debug("Invalid date:"+s),pt.debug("With date format:"+e.trim());const f=new Date(s);if(f===void 0||isNaN(f.getTime())||f.getFullYear()<-1e4||f.getFullYear()>1e4)throw new Error("Invalid date:"+s);return f}},ne=function(t){const e=/^(\d+(?:\.\d+)?)([Mdhmswy]|ms)$/.exec(t.trim());return e!==null?[Number.parseFloat(e[1]),e[2]]:[NaN,"ms"]},ie=function(t,e,s,r=!1){s=s.trim();const h=/^until\s+(?<ids>[\d\w- ]+)/.exec(s);if(h!==null){let D=null;for(const _ of h.groups.ids.split(" ")){let v=nt(_);v!==void 0&&(!D||v.startTime<D.startTime)&&(D=v)}if(D)return D.startTime;const S=new Date;return S.setHours(0,0,0,0),S}let f=j(s,e.trim(),!0);if(f.isValid())return r&&(f=f.add(1,"d")),f.toDate();let T=j(t);const[M,g]=ne(s);if(!Number.isNaN(M)){const D=T.add(M,g);D.isValid()&&(T=D)}return T.toDate()};let yt=0;const st=function(t){return t===void 0?(yt=yt+1,"task"+yt):t},xn=function(t,e){let s;e.substr(0,1)===":"?s=e.substr(1,e.length):s=e;const r=s.split(","),a={};oe(r,a,$t);for(let f=0;f<r.length;f++)r[f]=r[f].trim();let h="";switch(r.length){case 1:a.id=st(),a.startTime=t.endTime,h=r[0];break;case 2:a.id=st(),a.startTime=Dt(void 0,Z,r[0]),h=r[1];break;case 3:a.id=st(r[0]),a.startTime=Dt(void 0,Z,r[1]),h=r[2];break}return h&&(a.endTime=ie(a.startTime,Z,h,dt),a.manualEndTime=j(h,"YYYY-MM-DD",!0).isValid(),ee(a,Z,ut,lt)),a},vn=function(t,e){let s;e.substr(0,1)===":"?s=e.substr(1,e.length):s=e;const r=s.split(","),a={};oe(r,a,$t);for(let h=0;h<r.length;h++)r[h]=r[h].trim();switch(r.length){case 1:a.id=st(),a.startTime={type:"prevTaskEnd",id:t},a.endTime={data:r[0]};break;case 2:a.id=st(),a.startTime={type:"getStartDate",startData:r[0]},a.endTime={data:r[1]};break;case 3:a.id=st(r[0]),a.startTime={type:"getStartDate",startData:r[1]},a.endTime={data:r[2]};break}return a};let Ct,gt,B=[];const se={},bn=function(t,e){const s={section:rt,type:rt,processed:!1,manualEndTime:!1,renderEndTime:null,raw:{data:e},task:t,classes:[]},r=vn(gt,e);s.raw.startTime=r.startTime,s.raw.endTime=r.endTime,s.id=r.id,s.prevTaskId=gt,s.active=r.active,s.done=r.done,s.crit=r.crit,s.milestone=r.milestone,s.order=_t,_t++;const a=B.push(s);gt=s.id,se[s.id]=a-1},nt=function(t){const e=se[t];return B[e]},Tn=function(t,e){const s={section:rt,type:rt,description:t,task:t,classes:[]},r=xn(Ct,e);s.startTime=r.startTime,s.endTime=r.endTime,s.id=r.id,s.active=r.active,s.done=r.done,s.crit=r.crit,s.milestone=r.milestone,Ct=s,xt.push(s)},qt=function(){const t=function(s){const r=B[s];let a="";switch(B[s].raw.startTime.type){case"prevTaskEnd":{const h=nt(r.prevTaskId);r.startTime=h.endTime;break}case"getStartDate":a=Dt(void 0,Z,B[s].raw.startTime.startData),a&&(B[s].startTime=a);break}return B[s].startTime&&(B[s].endTime=ie(B[s].startTime,Z,B[s].raw.endTime.data,dt),B[s].endTime&&(B[s].processed=!0,B[s].manualEndTime=j(B[s].raw.endTime.data,"YYYY-MM-DD",!0).isValid(),ee(B[s],Z,ut,lt))),B[s].processed};let e=!0;for(const[s,r]of B.entries())t(s),e=e&&r.processed;return e},wn=function(t,e){let s=e;it().securityLevel!=="loose"&&(s=ke.sanitizeUrl(e)),t.split(",").forEach(function(r){nt(r)!==void 0&&(ae(r,()=>{window.open(s,"_self")}),It[r]=s)}),re(t,"clickable")},re=function(t,e){t.split(",").forEach(function(s){let r=nt(s);r!==void 0&&r.classes.push(e)})},_n=function(t,e,s){if(it().securityLevel!=="loose"||e===void 0)return;let r=[];if(typeof s=="string"){r=s.split(/,(?=(?:(?:[^"]*"){2})*[^"]*$)/);for(let h=0;h<r.length;h++){let f=r[h].trim();f.charAt(0)==='"'&&f.charAt(f.length-1)==='"'&&(f=f.substr(1,f.length-2)),r[h]=f}}r.length===0&&r.push(t),nt(t)!==void 0&&ae(t,()=>{pe.runFunc(e,...r)})},ae=function(t,e){Wt.push(function(){const s=document.querySelector(`[id="${t}"]`);s!==null&&s.addEventListener("click",function(){e()})},function(){const s=document.querySelector(`[id="${t}-text"]`);s!==null&&s.addEventListener("click",function(){e()})})},Dn=function(t,e,s){t.split(",").forEach(function(r){_n(r,e,s)}),re(t,"clickable")},Cn=function(t){Wt.forEach(function(e){e(t)})},Sn={getConfig:()=>it().gantt,clear:je,setDateFormat:$e,getDateFormat:on,enableInclusiveEndDates:tn,endDatesAreInclusive:en,enableTopAxis:nn,topAxisEnabled:sn,setAxisFormat:qe,getAxisFormat:Ue,setTickInterval:Ze,getTickInterval:Qe,setTodayMarker:Je,getTodayMarker:Ke,setAccTitle:ce,getAccTitle:le,setDiagramTitle:ue,getDiagramTitle:de,setDisplayMode:rn,getDisplayMode:an,setAccDescription:fe,getAccDescription:he,addSection:hn,getSections:mn,getTasks:kn,addTask:bn,findTaskById:nt,addTaskOrg:Tn,setIncludes:cn,getIncludes:ln,setExcludes:un,getExcludes:dn,setClickEvent:Dn,setLink:wn,getLinks:fn,bindFunctions:Cn,parseDuration:ne,isInvalidDate:te,setWeekday:yn,getWeekday:gn};function oe(t,e,s){let r=!0;for(;r;)r=!1,s.forEach(function(a){const h="^\\s*"+a+"\\s*$",f=new RegExp(h);t[0].match(f)&&(e[a]=!0,t.shift(1),r=!0)})}const En=function(){pt.debug("Something is calling, setConf, remove the call")},Ut={monday:_e,tuesday:De,wednesday:Ce,thursday:Se,friday:Ee,saturday:Me,sunday:Ae},Mn=(t,e)=>{let s=[...t].map(()=>-1/0),r=[...t].sort((h,f)=>h.startTime-f.startTime||h.order-f.order),a=0;for(const h of r)for(let f=0;f<s.length;f++)if(h.startTime>=s[f]){s[f]=h.endTime,h.order=f+e,f>a&&(a=f);break}return a};let $;const An=function(t,e,s,r){const a=it().gantt,h=it().securityLevel;let f;h==="sandbox"&&(f=ht("#i"+e));const T=h==="sandbox"?ht(f.nodes()[0].contentDocument.body):ht("body"),M=h==="sandbox"?f.nodes()[0].contentDocument:document,g=M.getElementById(e);$=g.parentElement.offsetWidth,$===void 0&&($=1200),a.useWidth!==void 0&&($=a.useWidth);const D=r.db.getTasks();let S=[];for(const k of D)S.push(k.type);S=z(S);const _={};let v=2*a.topPadding;if(r.db.getDisplayMode()==="compact"||a.displayMode==="compact"){const k={};for(const p of D)k[p.section]===void 0?k[p.section]=[p]:k[p.section].push(p);let w=0;for(const p of Object.keys(k)){const y=Mn(k[p],w)+1;w+=y,v+=y*(a.barHeight+a.barGap),_[p]=y}}else{v+=D.length*(a.barHeight+a.barGap);for(const k of S)_[k]=D.filter(w=>w.type===k).length}g.setAttribute("viewBox","0 0 "+$+" "+v);const H=T.select(`[id="${e}"]`),m=xe().domain([ve(D,function(k){return k.startTime}),be(D,function(k){return k.endTime})]).rangeRound([0,$-a.leftPadding-a.rightPadding]);function C(k,w){const p=k.startTime,y=w.startTime;let n=0;return p>y?n=1:p<y&&(n=-1),n}D.sort(C),I(D,$,v),ye(H,v,$,a.useMaxWidth),H.append("text").text(r.db.getDiagramTitle()).attr("x",$/2).attr("y",a.titleTopMargin).attr("class","titleText");function I(k,w,p){const y=a.barHeight,n=y+a.barGap,u=a.topPadding,d=a.leftPadding,o=Te().domain([0,S.length]).range(["#00B9FA","#F95002"]).interpolate(we);O(n,u,d,w,p,k,r.db.getExcludes(),r.db.getIncludes()),P(d,u,w,p),L(k,n,u,d,y,o,w),G(n,u),N(d,u,w,p)}function L(k,w,p,y,n,u,d){const x=[...new Set(k.map(l=>l.order))].map(l=>k.find(b=>b.order===l));H.append("g").selectAll("rect").data(x).enter().append("rect").attr("x",0).attr("y",function(l,b){return b=l.order,b*w+p-2}).attr("width",function(){return d-a.rightPadding/2}).attr("height",w).attr("class",function(l){for(const[b,V]of S.entries())if(l.type===V)return"section section"+b%a.numberSectionStyles;return"section section0"});const i=H.append("g").selectAll("rect").data(k).enter(),W=r.db.getLinks();if(i.append("rect").attr("id",function(l){return l.id}).attr("rx",3).attr("ry",3).attr("x",function(l){return l.milestone?m(l.startTime)+y+.5*(m(l.endTime)-m(l.startTime))-.5*n:m(l.startTime)+y}).attr("y",function(l,b){return b=l.order,b*w+p}).attr("width",function(l){return l.milestone?n:m(l.renderEndTime||l.endTime)-m(l.startTime)}).attr("height",n).attr("transform-origin",function(l,b){return b=l.order,(m(l.startTime)+y+.5*(m(l.endTime)-m(l.startTime))).toString()+"px "+(b*w+p+.5*n).toString()+"px"}).attr("class",function(l){const b="task";let V="";l.classes.length>0&&(V=l.classes.join(" "));let A=0;for(const[E,F]of S.entries())l.type===F&&(A=E%a.numberSectionStyles);let Y="";return l.active?l.crit?Y+=" activeCrit":Y=" active":l.done?l.crit?Y=" doneCrit":Y=" done":l.crit&&(Y+=" crit"),Y.length===0&&(Y=" task"),l.milestone&&(Y=" milestone "+Y),Y+=A,Y+=" "+V,b+Y}),i.append("text").attr("id",function(l){return l.id+"-text"}).text(function(l){return l.task}).attr("font-size",a.fontSize).attr("x",function(l){let b=m(l.startTime),V=m(l.renderEndTime||l.endTime);l.milestone&&(b+=.5*(m(l.endTime)-m(l.startTime))-.5*n),l.milestone&&(V=b+n);const A=this.getBBox().width;return A>V-b?V+A+1.5*a.leftPadding>d?b+y-5:V+y+5:(V-b)/2+b+y}).attr("y",function(l,b){return b=l.order,b*w+a.barHeight/2+(a.fontSize/2-2)+p}).attr("text-height",n).attr("class",function(l){const b=m(l.startTime);let V=m(l.endTime);l.milestone&&(V=b+n);const A=this.getBBox().width;let Y="";l.classes.length>0&&(Y=l.classes.join(" "));let E=0;for(const[tt,Q]of S.entries())l.type===Q&&(E=tt%a.numberSectionStyles);let F="";return l.active&&(l.crit?F="activeCritText"+E:F="activeText"+E),l.done?l.crit?F=F+" doneCritText"+E:F=F+" doneText"+E:l.crit&&(F=F+" critText"+E),l.milestone&&(F+=" milestoneText"),A>V-b?V+A+1.5*a.leftPadding>d?Y+" taskTextOutsideLeft taskTextOutside"+E+" "+F:Y+" taskTextOutsideRight taskTextOutside"+E+" "+F+" width-"+A:Y+" taskText taskText"+E+" "+F+" width-"+A}),it().securityLevel==="sandbox"){let l;l=ht("#i"+e);const b=l.nodes()[0].contentDocument;i.filter(function(V){return W[V.id]!==void 0}).each(function(V){var A=b.querySelector("#"+V.id),Y=b.querySelector("#"+V.id+"-text");const E=A.parentNode;var F=b.createElement("a");F.setAttribute("xlink:href",W[V.id]),F.setAttribute("target","_top"),E.appendChild(F),F.appendChild(A),F.appendChild(Y)})}}function O(k,w,p,y,n,u,d,o){if(d.length===0&&o.length===0)return;let x,i;for(const{startTime:A,endTime:Y}of u)(x===void 0||A<x)&&(x=A),(i===void 0||Y>i)&&(i=Y);if(!x||!i)return;if(j(i).diff(j(x),"year")>5){pt.warn("The difference between the min and max time is more than 5 years. This will cause performance issues. Skipping drawing exclude days.");return}const W=r.db.getDateFormat(),c=[];let l=null,b=j(x);for(;b.valueOf()<=i;)r.db.isInvalidDate(b,W,d,o)?l?l.end=b:l={start:b,end:b}:l&&(c.push(l),l=null),b=b.add(1,"d");H.append("g").selectAll("rect").data(c).enter().append("rect").attr("id",function(A){return"exclude-"+A.start.format("YYYY-MM-DD")}).attr("x",function(A){return m(A.start)+p}).attr("y",a.gridLineStartPadding).attr("width",function(A){const Y=A.end.add(1,"day");return m(Y)-m(A.start)}).attr("height",n-w-a.gridLineStartPadding).attr("transform-origin",function(A,Y){return(m(A.start)+p+.5*(m(A.end)-m(A.start))).toString()+"px "+(Y*k+.5*n).toString()+"px"}).attr("class","exclude-range")}function P(k,w,p,y){let n=Oe(m).tickSize(-y+w+a.gridLineStartPadding).tickFormat(Pt(r.db.getAxisFormat()||a.axisFormat||"%Y-%m-%d"));const d=/^([1-9]\d*)(millisecond|second|minute|hour|day|week|month)$/.exec(r.db.getTickInterval()||a.tickInterval);if(d!==null){const o=d[1],x=d[2],i=r.db.getWeekday()||a.weekday;switch(x){case"millisecond":n.ticks(Xt.every(o));break;case"second":n.ticks(Gt.every(o));break;case"minute":n.ticks(Ht.every(o));break;case"hour":n.ticks(Bt.every(o));break;case"day":n.ticks(Rt.every(o));break;case"week":n.ticks(Ut[i].every(o));break;case"month":n.ticks(Nt.every(o));break}}if(H.append("g").attr("class","grid").attr("transform","translate("+k+", "+(y-50)+")").call(n).selectAll("text").style("text-anchor","middle").attr("fill","#000").attr("stroke","none").attr("font-size",10).attr("dy","1em"),r.db.topAxisEnabled()||a.topAxis){let o=ze(m).tickSize(-y+w+a.gridLineStartPadding).tickFormat(Pt(r.db.getAxisFormat()||a.axisFormat||"%Y-%m-%d"));if(d!==null){const x=d[1],i=d[2],W=r.db.getWeekday()||a.weekday;switch(i){case"millisecond":o.ticks(Xt.every(x));break;case"second":o.ticks(Gt.every(x));break;case"minute":o.ticks(Ht.every(x));break;case"hour":o.ticks(Bt.every(x));break;case"day":o.ticks(Rt.every(x));break;case"week":o.ticks(Ut[W].every(x));break;case"month":o.ticks(Nt.every(x));break}}H.append("g").attr("class","grid").attr("transform","translate("+k+", "+w+")").call(o).selectAll("text").style("text-anchor","middle").attr("fill","#000").attr("stroke","none").attr("font-size",10)}}function G(k,w){let p=0;const y=Object.keys(_).map(n=>[n,_[n]]);H.append("g").selectAll("text").data(y).enter().append(function(n){const u=n[0].split(ge.lineBreakRegex),d=-(u.length-1)/2,o=M.createElementNS("http://www.w3.org/2000/svg","text");o.setAttribute("dy",d+"em");for(const[x,i]of u.entries()){const W=M.createElementNS("http://www.w3.org/2000/svg","tspan");W.setAttribute("alignment-baseline","central"),W.setAttribute("x","10"),x>0&&W.setAttribute("dy","1em"),W.textContent=i,o.appendChild(W)}return o}).attr("x",10).attr("y",function(n,u){if(u>0)for(let d=0;d<u;d++)return p+=y[u-1][1],n[1]*k/2+p*k+w;else return n[1]*k/2+w}).attr("font-size",a.sectionFontSize).attr("class",function(n){for(const[u,d]of S.entries())if(n[0]===d)return"sectionTitle sectionTitle"+u%a.numberSectionStyles;return"sectionTitle"})}function N(k,w,p,y){const n=r.db.getTodayMarker();if(n==="off")return;const u=H.append("g").attr("class","today"),d=new Date,o=u.append("line");o.attr("x1",m(d)+k).attr("x2",m(d)+k).attr("y1",a.titleTopMargin).attr("y2",y-a.titleTopMargin).attr("class","today"),n!==""&&o.attr("style",n.replace(/,/g,";"))}function z(k){const w={},p=[];for(let y=0,n=k.length;y<n;++y)Object.prototype.hasOwnProperty.call(w,k[y])||(w[k[y]]=!0,p.push(k[y]));return p}},Ln={setConf:En,draw:An},In=t=>`
  .mermaid-main-font {
    font-family: var(--mermaid-font-family, "trebuchet ms", verdana, arial, sans-serif);
  }

  .exclude-range {
    fill: ${t.excludeBkgColor};
  }

  .section {
    stroke: none;
    opacity: 0.2;
  }

  .section0 {
    fill: ${t.sectionBkgColor};
  }

  .section2 {
    fill: ${t.sectionBkgColor2};
  }

  .section1,
  .section3 {
    fill: ${t.altSectionBkgColor};
    opacity: 0.2;
  }

  .sectionTitle0 {
    fill: ${t.titleColor};
  }

  .sectionTitle1 {
    fill: ${t.titleColor};
  }

  .sectionTitle2 {
    fill: ${t.titleColor};
  }

  .sectionTitle3 {
    fill: ${t.titleColor};
  }

  .sectionTitle {
    text-anchor: start;
    font-family: var(--mermaid-font-family, "trebuchet ms", verdana, arial, sans-serif);
  }


  /* Grid and axis */

  .grid .tick {
    stroke: ${t.gridColor};
    opacity: 0.8;
    shape-rendering: crispEdges;
  }

  .grid .tick text {
    font-family: ${t.fontFamily};
    fill: ${t.textColor};
  }

  .grid path {
    stroke-width: 0;
  }


  /* Today line */

  .today {
    fill: none;
    stroke: ${t.todayLineColor};
    stroke-width: 2px;
  }


  /* Task styling */

  /* Default task */

  .task {
    stroke-width: 2;
  }

  .taskText {
    text-anchor: middle;
    font-family: var(--mermaid-font-family, "trebuchet ms", verdana, arial, sans-serif);
  }

  .taskTextOutsideRight {
    fill: ${t.taskTextDarkColor};
    text-anchor: start;
    font-family: var(--mermaid-font-family, "trebuchet ms", verdana, arial, sans-serif);
  }

  .taskTextOutsideLeft {
    fill: ${t.taskTextDarkColor};
    text-anchor: end;
  }


  /* Special case clickable */

  .task.clickable {
    cursor: pointer;
  }

  .taskText.clickable {
    cursor: pointer;
    fill: ${t.taskTextClickableColor} !important;
    font-weight: bold;
  }

  .taskTextOutsideLeft.clickable {
    cursor: pointer;
    fill: ${t.taskTextClickableColor} !important;
    font-weight: bold;
  }

  .taskTextOutsideRight.clickable {
    cursor: pointer;
    fill: ${t.taskTextClickableColor} !important;
    font-weight: bold;
  }


  /* Specific task settings for the sections*/

  .taskText0,
  .taskText1,
  .taskText2,
  .taskText3 {
    fill: ${t.taskTextColor};
  }

  .task0,
  .task1,
  .task2,
  .task3 {
    fill: ${t.taskBkgColor};
    stroke: ${t.taskBorderColor};
  }

  .taskTextOutside0,
  .taskTextOutside2
  {
    fill: ${t.taskTextOutsideColor};
  }

  .taskTextOutside1,
  .taskTextOutside3 {
    fill: ${t.taskTextOutsideColor};
  }


  /* Active task */

  .active0,
  .active1,
  .active2,
  .active3 {
    fill: ${t.activeTaskBkgColor};
    stroke: ${t.activeTaskBorderColor};
  }

  .activeText0,
  .activeText1,
  .activeText2,
  .activeText3 {
    fill: ${t.taskTextDarkColor} !important;
  }


  /* Completed task */

  .done0,
  .done1,
  .done2,
  .done3 {
    stroke: ${t.doneTaskBorderColor};
    fill: ${t.doneTaskBkgColor};
    stroke-width: 2;
  }

  .doneText0,
  .doneText1,
  .doneText2,
  .doneText3 {
    fill: ${t.taskTextDarkColor} !important;
  }


  /* Tasks on the critical line */

  .crit0,
  .crit1,
  .crit2,
  .crit3 {
    stroke: ${t.critBorderColor};
    fill: ${t.critBkgColor};
    stroke-width: 2;
  }

  .activeCrit0,
  .activeCrit1,
  .activeCrit2,
  .activeCrit3 {
    stroke: ${t.critBorderColor};
    fill: ${t.activeTaskBkgColor};
    stroke-width: 2;
  }

  .doneCrit0,
  .doneCrit1,
  .doneCrit2,
  .doneCrit3 {
    stroke: ${t.critBorderColor};
    fill: ${t.doneTaskBkgColor};
    stroke-width: 2;
    cursor: pointer;
    shape-rendering: crispEdges;
  }

  .milestone {
    transform: rotate(45deg) scale(0.8,0.8);
  }

  .milestoneText {
    font-style: italic;
  }
  .doneCritText0,
  .doneCritText1,
  .doneCritText2,
  .doneCritText3 {
    fill: ${t.taskTextDarkColor} !important;
  }

  .activeCritText0,
  .activeCritText1,
  .activeCritText2,
  .activeCritText3 {
    fill: ${t.taskTextDarkColor} !important;
  }

  .titleText {
    text-anchor: middle;
    font-size: 18px;
    fill: ${t.titleColor||t.textColor};
    font-family: var(--mermaid-font-family, "trebuchet ms", verdana, arial, sans-serif);
  }
`,Yn=In,Vn={parser:Xe,db:Sn,renderer:Ln,styles:Yn};export{Vn as diagram};
