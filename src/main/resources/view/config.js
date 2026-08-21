import { GraphicEntityModule } from './entity-module/GraphicEntityModule.js';
import { ViewportModule } from './viewport-module/ViewportModule.js';
import { TooltipModule } from './tooltip-module/TooltipModule.js';
import { ToggleModule } from './toggle-module/ToggleModule.js'

export const modules = [
    GraphicEntityModule,
    ViewportModule,
    TooltipModule,
    ToggleModule
];

// The list of toggles displayed in the options of the viewer
export const options = [
  ToggleModule.defineToggle({
    toggle: 'debug',
    title: 'DEBUG MODE',
    values: {
      'ON': true,
      'OFF': false
    },
    default: true
  })
]