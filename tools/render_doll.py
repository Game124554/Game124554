"""Preview of the paper doll: a line-for-line port of client/PaperDoll.java to Pillow."""
from PIL import Image, ImageDraw, ImageFont
W,H,EARS,GROUPS=16,32,3,6
SKIN=(0xE3,0xB3,0x8F); SKIN_LINE=(0x6B,0x4A,0x36)
BG=(0x24,0x24,0x2C); BGL=(0x4A,0x4A,0x58); BL=(0x08,0x08,0x0B)
WG=(0xF2,0xF2,0xF6); WGS=(0xC9,0xC9,0xD4); WL=(0x77,0x77,0x84)
PINK=(0xE8,0x8A,0xA4); GRAY=(0x85,0x85,0x8F)
PARTS=[("right_arm",0,8,4,9,0,False),("right_hand",0,17,4,3,0,True),("left_arm",12,8,4,9,1,False),("left_hand",12,17,4,3,1,True),
       ("right_leg",4,20,4,9,2,False),("right_foot",4,29,4,3,2,True),("left_leg",8,20,4,9,3,False),("left_foot",8,29,4,3,3,True),
       ("body",4,8,8,12,4,False),("head",4,0,8,8,5,False)]
FURRY_AT=34.0
def state(order,pct,tf):
    if tf: return 2
    if pct<=0: return 0
    if pct>=FURRY_AT+order*(100-FURRY_AT)/GROUPS: return 2
    return 1 if pct>order*FURRY_AT/GROUPS else 0
def blend(a,b,t): return tuple(round(a[i]*(1-t)+b[i]*t) for i in range(3))
def draw(d,x,y,s,pct,white,tf):
    goo=WG if white else BG; line=WL if white else BL; tick=WGS if white else BGL; pad=GRAY if white else PINK
    def rect(ux,uy,uw,uh,c):
        x0=x+round(ux*s); y0=y+round(uy*s); x1=max(x0+1,x+round((ux+uw)*s)); y1=max(y0+1,y+round((uy+uh)*s))
        d.rectangle([x0,y0,x1-1,y1-1],fill=c)
    def box(ux,uy,uw,uh,l,f):
        x0=x+round(ux*s); y0=y+round(uy*s); x1=x+round((ux+uw)*s); y1=y+round((uy+uh)*s)
        d.rectangle([x0,y0,x1-1,y1-1],fill=l)
        if x1-x0>2 and y1-y0>2: d.rectangle([x0+1,y0+1,x1-2,y1-2],fill=f)
    head=state(5,pct,tf)==2; body=state(4,pct,tf)==2
    if body:
        box(11,20.5,3.5,2,line,goo); box(13.5,22,2.5,2.5,line,goo); box(14.5,24,2,3.5,line,goo)
    if head:
        box(4,-EARS,2.5,EARS+.5,line,goo); box(9.5,-EARS,2.5,EARS+.5,line,goo)
        rect(4.8,-EARS+1,.9,1.6,pad); rect(10.3,-EARS+1,.9,1.6,pad)
    for n,px,py,pw,ph,o,paw in PARTS:
        st=state(o,pct,tf)
        if st==0: box(px,py,pw,ph,SKIN_LINE,SKIN)
        elif st==1:
            box(px,py,pw,ph,SKIN_LINE,blend(SKIN,goo,.25))
            rect(px+pw/2-.8,py+ph/2-.8,1.6,1.6,blend(SKIN,goo,.8)); rect(px+pw/2-.3,py+ph/2+.8,.6,.9,blend(SKIN,goo,.8))
        else:
            box(px,py,pw,ph,line,goo)
            if not paw:
                fy=py+1.5
                while fy<py+ph-1:
                    rect(px+.8,fy,.7,.9,tick); rect(px+pw-1.5,fy+1.2,.7,.9,tick); fy+=3
            else:
                rect(px+.5,py+.45,.8,.8,pad); rect(px+1.6,py+.3,.8,.8,pad); rect(px+2.7,py+.45,.8,.8,pad); rect(px+1.1,py+1.4,1.8,1.2,pad)
    eye=(BL if white else (0xF2,0xF2,0xF6)) if head else (0x2A,0x1C,0x14)
    rect(5.8,3.2,1.2,1.2,eye); rect(9,3.2,1.2,1.2,eye)
    if head: rect(7.4,5,1.2,.9,pad)
cases=[("Human body\n0%",0,False,False),("Goo inside\n20%",20,False,False),("Furry body\n45%  hands = paws",45,False,False),
       ("Transfurring\n80%",80,False,False),("Transfurred",100,False,True),("White goo\n45%",45,True,False),("White latex\ntransfurred",100,True,True)]
S=6; cw=W*S+44; img=Image.new("RGB",(cw*len(cases)+20,(H+EARS)*S+150),(58,62,70)); d=ImageDraw.Draw(img)
try: font=ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",13)
except Exception: font=ImageFont.load_default()
for i,(label,pct,white,tf) in enumerate(cases):
    x=20+i*cw; y=20+EARS*S
    d.rectangle([x-8,y-EARS*S-8,x+W*S+8,y+H*S+8],fill=(28,30,36))
    draw(d,x,y,S,pct,white,tf)
    d.multiline_text((x-6,y+H*S+16),label,fill=(235,235,235),font=font,spacing=3)
# HUD size (scale 2) samples
for i,(label,pct,white,tf) in enumerate(cases):
    x=20+i*cw+W*S-28; y=(H+EARS)*S+70+EARS*2
    d.rectangle([x-3,y-EARS*2-3,x+W*2+3,y+H*2+3],fill=(20,20,24))
    draw(d,x,y,2,pct,white,tf)
d.text((20,(H+EARS)*S+128),"small figures = size on the HUD (scale 2)",fill=(170,170,170),font=font)
img.save("/home/claude/preview/paper_doll.png"); print(img.size)
