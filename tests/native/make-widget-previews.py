#!/usr/bin/env python3
"""Arrange native widget PNG renders for review; never redraw the widget UI.
Usage: python3 make-widget-previews.py SCREENSHOT_WIDGET_DIRECTORY OUTPUT_DIRECTORY
"""
from pathlib import Path
import sys
from PIL import Image, ImageDraw, ImageFont
src,out=map(Path,sys.argv[1:3]);out.mkdir(parents=True,exist_ok=True)
fontfile='/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf'
font=lambda n:ImageFont.truetype(fontfile,n)
for language in ('en','ar'):
 im=Image.new('RGB',(1240,1540),'#f6f4ee');d=ImageDraw.Draw(im)
 d.text((48,28),'Salah 2.1 · '+('English' if language=='en' else 'Arabic')+' widgets',font=font(35),fill='#153f35')
 d.text((48,80),'Native Android renders · actual widget content at 2× for readability',font=font(19),fill='#65736c')
 panels=[('2×2 · 160×160 dp','compact-normal-160x160',48,165),('2×3 · 160×240 dp','tall-normal-160x240',416,165),('Resized · 200×300 dp','compact-normal-200x300',784,165),('Estimated iqamah','tall-phase2-0',200,860),('Seconds hidden · approximate HH:MM','tall-hide-seconds',660,860)]
 for label,name,x,y in panels:
  d.text((x,y-38),label,font=font(20 if len(label)<30 else 17),fill='#213a34')
  native=Image.open(src/f'{language}-{name}.png').convert('RGBA')
  scaled=native.resize((native.width*2,native.height*2),Image.Resampling.LANCZOS)
  im.paste(scaled,(x,y),scaled)
 d.text((48,1490),'Host-side preview, not a physical-phone screenshot. Launcher grid and padding vary.',font=font(18),fill='#65736c')
 im.save(out/f'Salah-Abu-Dhabi-v2.1-Widgets-{language.upper()}.png')
