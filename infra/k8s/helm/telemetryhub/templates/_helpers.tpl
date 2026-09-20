{{- define "telemetryhub.labels" -}}
app.kubernetes.io/name: {{ .Chart.Name }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end -}}

{{- define "telemetryhub.fullname" -}}
{{ .Release.Name }}-telemetryhub
{{- end -}}

{{- define "telemetryhub.tags" -}}
{{- range $_, $tag := .Values.commonTags }}
{{ $tag.key }}: {{ $tag.value }}
{{- end }}
{{- end -}}