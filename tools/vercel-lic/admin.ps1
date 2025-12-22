<#
.SYNOPSIS
  Nexus Visual License Admin Tool
  Управление ключами активации

.PARAMETER Action
  Действие: create, list, revoke, unrevoke, delete, extend, reset_hwid

.PARAMETER BaseUrl
  URL сервера, например https://nexus-visual-rose.vercel.app/api

.PARAMETER AdminToken
  Токен администратора (NV_ADMIN_TOKEN)

.PARAMETER Code
  Код ключа для операций (revoke, unrevoke, delete, extend, reset_hwid)

.PARAMETER Days
  Количество дней для create (duration) или extend

.PARAMETER Note
  Заметка при создании ключа

.PARAMETER Reason
  Причина отзыва ключа

.EXAMPLE
  # Создать ключ на 7 дней
  ./admin.ps1 -Action create -BaseUrl "https://..." -AdminToken "..." -Days 7

  # Создать бессрочный ключ
  ./admin.ps1 -Action create -BaseUrl "https://..." -AdminToken "..."

  # Список всех ключей
  ./admin.ps1 -Action list -BaseUrl "https://..." -AdminToken "..."

  # Отозвать ключ
  ./admin.ps1 -Action revoke -BaseUrl "https://..." -AdminToken "..." -Code "NV-XXXX-XXXX-XXXX" -Reason "Bad behavior"

  # Вернуть отозванный ключ
  ./admin.ps1 -Action unrevoke -BaseUrl "https://..." -AdminToken "..." -Code "NV-XXXX-XXXX-XXXX"

  # Продлить ключ на 30 дней
  ./admin.ps1 -Action extend -BaseUrl "https://..." -AdminToken "..." -Code "NV-XXXX-XXXX-XXXX" -Days 30

  # Сбросить HWID (игрок сможет активировать на другом ПК)
  ./admin.ps1 -Action reset_hwid -BaseUrl "https://..." -AdminToken "..." -Code "NV-XXXX-XXXX-XXXX"

  # Удалить ключ полностью
  ./admin.ps1 -Action delete -BaseUrl "https://..." -AdminToken "..." -Code "NV-XXXX-XXXX-XXXX"
#>

param(
    [Parameter(Mandatory=$true)]
    [ValidateSet("create", "list", "revoke", "unrevoke", "delete", "extend", "reset_hwid")]
    [string]$Action,

    [Parameter(Mandatory=$true)]
    [string]$BaseUrl,

    [Parameter(Mandatory=$true)]
    [string]$AdminToken,

    [string]$Code,
    [int]$Days = 0,
    [string]$Note,
    [string]$Reason
)

$headers = @{
    "Authorization" = "Bearer $AdminToken"
    "Content-Type" = "application/json"
}

function Invoke-Api {
    param($Endpoint, $Body = @{}, $Method = "POST")
    
    $url = "$BaseUrl/$Endpoint"
    $json = $Body | ConvertTo-Json -Compress
    
    try {
        if ($Method -eq "GET") {
            $response = Invoke-RestMethod -Uri $url -Method GET -Headers $headers
        } else {
            $response = Invoke-RestMethod -Uri $url -Method POST -Headers $headers -Body $json
        }
        return $response
    } catch {
        $err = $_.ErrorDetails.Message
        if ($err) {
            Write-Host "Error: $err" -ForegroundColor Red
        } else {
            Write-Host "Error: $_" -ForegroundColor Red
        }
        return $null
    }
}

switch ($Action) {
    "create" {
        $body = @{}
        if ($Days -gt 0) {
            $body["duration_days"] = $Days
        }
        if ($Note) {
            $body["note"] = $Note
        }
        
        $result = Invoke-Api "admin_create_code" $body
        if ($result) {
            Write-Host ""
            Write-Host "=== НОВЫЙ КЛЮЧ СОЗДАН ===" -ForegroundColor Green
            Write-Host "Код: $($result.code)" -ForegroundColor Cyan
            if ($result.duration_days) {
                Write-Host "Срок действия: $($result.duration_days) дней (начнётся после активации)" -ForegroundColor Yellow
            } else {
                Write-Host "Срок действия: БЕССРОЧНО" -ForegroundColor Yellow
            }
            if ($result.note) {
                Write-Host "Заметка: $($result.note)" -ForegroundColor Gray
            }
            Write-Host ""
        }
    }
    
    "list" {
        $result = Invoke-Api "admin_list" @{} "GET"
        if ($result) {
            Write-Host ""
            Write-Host "=== СПИСОК КЛЮЧЕЙ ($($result.total) шт) ===" -ForegroundColor Green
            Write-Host ""
            
            foreach ($c in $result.codes) {
                $statusColor = switch ($c.status) {
                    "active" { "Green" }
                    "unused" { "Yellow" }
                    "expired" { "DarkGray" }
                    "revoked" { "Red" }
                    default { "White" }
                }
                
                Write-Host "[$($c.status.ToUpper())]" -ForegroundColor $statusColor -NoNewline
                Write-Host " $($c.code)" -ForegroundColor Cyan
                
                if ($c.bound_uuid) {
                    Write-Host "  UUID: $($c.bound_uuid)" -ForegroundColor Gray
                }
                if ($c.expires_at) {
                    $expDate = [DateTimeOffset]::FromUnixTimeSeconds($c.expires_at).LocalDateTime
                    Write-Host "  Истекает: $expDate" -ForegroundColor $(if ($c.expired) { "Red" } else { "Yellow" })
                }
                if ($c.revoke_reason) {
                    Write-Host "  Причина отзыва: $($c.revoke_reason)" -ForegroundColor Red
                }
                if ($c.note) {
                    Write-Host "  Заметка: $($c.note)" -ForegroundColor DarkGray
                }
                Write-Host ""
            }
        }
    }
    
    "revoke" {
        if (-not $Code) {
            Write-Host "Error: укажи -Code" -ForegroundColor Red
            return
        }
        
        $body = @{ "code" = $Code }
        if ($Reason) {
            $body["reason"] = $Reason
        }
        
        $result = Invoke-Api "admin_revoke" $body
        if ($result -and $result.success) {
            Write-Host ""
            Write-Host "=== КЛЮЧ ОТОЗВАН ===" -ForegroundColor Red
            Write-Host "Код: $Code" -ForegroundColor Cyan
            if ($Reason) {
                Write-Host "Причина: $Reason" -ForegroundColor Yellow
            }
            Write-Host "Игрок больше не сможет использовать этот ключ" -ForegroundColor Gray
            Write-Host ""
        }
    }
    
    "unrevoke" {
        if (-not $Code) {
            Write-Host "Error: укажи -Code" -ForegroundColor Red
            return
        }
        
        $result = Invoke-Api "admin_unrevoke" @{ "code" = $Code }
        if ($result -and $result.success) {
            Write-Host ""
            Write-Host "=== ОТЗЫВ СНЯТ ===" -ForegroundColor Green
            Write-Host "Код: $Code" -ForegroundColor Cyan
            Write-Host "Ключ снова активен" -ForegroundColor Gray
            Write-Host ""
        }
    }
    
    "delete" {
        if (-not $Code) {
            Write-Host "Error: укажи -Code" -ForegroundColor Red
            return
        }
        
        $result = Invoke-Api "admin_delete" @{ "code" = $Code }
        if ($result -and $result.success) {
            Write-Host ""
            Write-Host "=== КЛЮЧ УДАЛЁН ===" -ForegroundColor Red
            Write-Host "Код: $Code" -ForegroundColor Cyan
            Write-Host "Ключ полностью удалён из базы данных" -ForegroundColor Gray
            Write-Host ""
        }
    }
    
    "extend" {
        if (-not $Code) {
            Write-Host "Error: укажи -Code" -ForegroundColor Red
            return
        }
        if ($Days -le 0) {
            Write-Host "Error: укажи -Days (больше 0)" -ForegroundColor Red
            return
        }
        
        $result = Invoke-Api "admin_extend" @{ "code" = $Code; "days" = $Days }
        if ($result -and $result.success) {
            Write-Host ""
            Write-Host "=== КЛЮЧ ПРОДЛЁН ===" -ForegroundColor Green
            Write-Host "Код: $Code" -ForegroundColor Cyan
            Write-Host "Добавлено: $Days дней" -ForegroundColor Yellow
            Write-Host "Новая дата истечения: $($result.expires_date)" -ForegroundColor Gray
            Write-Host ""
        }
    }
    
    "reset_hwid" {
        if (-not $Code) {
            Write-Host "Error: укажи -Code" -ForegroundColor Red
            return
        }
        
        $result = Invoke-Api "admin_reset_hwid" @{ "code" = $Code }
        if ($result -and $result.success) {
            Write-Host ""
            Write-Host "=== HWID СБРОШЕН ===" -ForegroundColor Green
            Write-Host "Код: $Code" -ForegroundColor Cyan
            Write-Host "Игрок сможет активировать ключ на другом ПК" -ForegroundColor Gray
            Write-Host ""
        }
    }
}
