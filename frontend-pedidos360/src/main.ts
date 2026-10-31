import { bootstrapApplication } from '@angular/platform-browser';
import { createAppConfig } from './app/app.config';
import { App } from './app/app';
import { loadAppConfig } from './app/core/config/app-config';

// 1) Cargar /config.json  2) Iniciar Angular con esa configuración (MSAL, URL del backend)
loadAppConfig()
  .then(() => bootstrapApplication(App, createAppConfig()))
  .catch((err) => {
    console.error(err);
    document.body.innerHTML =
      '<p style="font-family:sans-serif;padding:2rem">No fue posible iniciar Pedidos360: ' +
      'revise el archivo config.json.</p>';
  });
