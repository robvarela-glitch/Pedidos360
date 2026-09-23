import { Component, OnInit, inject, signal } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';

interface UserProfile {
    id?: string;
    displayName?: string;
    givenName?: string;
    surname?: string;
    mail?: string;
    userPrincipalName?: string;
    jobTitle?: string;
    department?: string;
    companyName?: string;
    officeLocation?: string;
    city?: string;
    country?: string;
    mobilePhone?: string;
    businessPhones?: string[];
    preferredLanguage?: string;
}

// Campos solicitados explícitamente para reducir el tamaño de la respuesta
const SELECT_FIELDS = [
    'id', 'displayName', 'givenName', 'surname',
    'mail', 'userPrincipalName', 'jobTitle', 'department',
    'companyName', 'officeLocation', 'city', 'country',
    'mobilePhone', 'businessPhones', 'preferredLanguage'
].join(',');

@Component({
    selector: 'app-profile',
    standalone: true,
    template: `
        <div style="padding: 20px;">
        <h2>Perfil del Usuario</h2>

        @if (profile(); as p) {
            <table>
            @if (p.displayName) {
                <tr><th>Nombre</th><td>{{ p.displayName }}</td></tr>
            }
            @if (p.givenName || p.surname) {
                <tr><th>Nombre / Apellido</th><td>{{ p.givenName }} {{ p.surname }}</td></tr>
            }
            @if (p.mail || p.userPrincipalName) {
                <tr><th>Correo</th><td>{{ p.mail || p.userPrincipalName }}</td></tr>
            }
            @if (p.jobTitle) {
                <tr><th>Puesto</th><td>{{ p.jobTitle }}</td></tr>
            }
            @if (p.department) {
                <tr><th>Departamento</th><td>{{ p.department }}</td></tr>
            }
            @if (p.companyName) {
                <tr><th>Empresa</th><td>{{ p.companyName }}</td></tr>
            }
            @if (p.officeLocation) {
                <tr><th>Oficina</th><td>{{ p.officeLocation }}</td></tr>
            }
            @if (p.city || p.country) {
                <tr><th>Ubicación</th><td>{{ p.city }}{{ p.city && p.country ? ', ' : '' }}{{ p.country }}</td></tr>
            }
            @if (p.mobilePhone) {
                <tr><th>Móvil</th><td>{{ p.mobilePhone }}</td></tr>
            }
            @if (p.businessPhones?.length) {
                <tr><th>Teléfono</th><td>{{ p.businessPhones!.join(', ') }}</td></tr>
            }
            @if (p.preferredLanguage) {
                <tr><th>Idioma</th><td>{{ p.preferredLanguage }}</td></tr>
            }
            @if (p.id) {
                <tr><th>ID de objeto</th><td>{{ p.id }}</td></tr>
            }
            </table>
        } @else if (error()) {
            <p style="color: red;">Error al cargar el perfil: {{ error() }}</p>
        } @else {
            <p>Cargando datos del perfil desde Microsoft Graph...</p>
        }
        </div>
    `
})
export class Profile implements OnInit {
    private http = inject(HttpClient);
    profile = signal<UserProfile | null>(null);
    error = signal<string | null>(null);

    ngOnInit(): void {
        const params = new HttpParams().set('$select', SELECT_FIELDS);
        this.http.get<UserProfile>('https://graph.microsoft.com/v1.0/me', { params })
        .subscribe({
            next: (data) => this.profile.set(data),
            error: (err) => this.error.set(err?.message ?? 'Error desconocido')
        });
    }
}
