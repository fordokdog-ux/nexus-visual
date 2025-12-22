<#
.SYNOPSIS
  Nexus Visual License Admin Tool

.PARAMETER Action
  Action: create, list, revoke, unrevoke, delete, extend, reset_hwid

.PARAMETER BaseUrl
  Server URL, e.g. https://nexus-visual-rose.vercel.app/api

.PARAMETER AdminToken
  Admin token (NV_ADMIN_TOKEN)

.PARAMETER Code
  Key code for operations

.PARAMETER Days
  Number of days for create/extend

.PARAMETER Note
  Note when creating key

.PARAMETER Reason
  Reason for revoke
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
            Write-Host "=== KEY CREATED ===" -ForegroundColor Green
            Write-Host "Code: $($result.code)" -ForegroundColor Cyan
            if ($result.duration_days) {
                Write-Host "Duration: $($result.duration_days) days (starts after activation)" -ForegroundColor Yellow
            } else {
                Write-Host "Duration: PERMANENT" -ForegroundColor Yellow
            }
            if ($result.note) {
                Write-Host "Note: $($result.note)" -ForegroundColor Gray
            }
            Write-Host ""
        }
    }
    
    "list" {
        $result = Invoke-Api "admin_list" @{} "GET"
        if ($result) {
            Write-Host ""
            Write-Host "=== KEY LIST ($($result.total) total) ===" -ForegroundColor Green
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
                    $expColor = if ($c.expired) { "Red" } else { "Yellow" }
                    Write-Host "  Expires: $expDate" -ForegroundColor $expColor
                }
                if ($c.revoke_reason) {
                    Write-Host "  Revoke reason: $($c.revoke_reason)" -ForegroundColor Red
                }
                if ($c.note) {
                    Write-Host "  Note: $($c.note)" -ForegroundColor DarkGray
                }
                Write-Host ""
            }
        }
    }
    
    "revoke" {
        if (-not $Code) {
            Write-Host "Error: specify -Code" -ForegroundColor Red
            return
        }
        
        $body = @{ "code" = $Code }
        if ($Reason) {
            $body["reason"] = $Reason
        }
        
        $result = Invoke-Api "admin_revoke" $body
        if ($result -and $result.success) {
            Write-Host ""
            Write-Host "=== KEY REVOKED ===" -ForegroundColor Red
            Write-Host "Code: $Code" -ForegroundColor Cyan
            if ($Reason) {
                Write-Host "Reason: $Reason" -ForegroundColor Yellow
            }
            Write-Host "Player can no longer use this key" -ForegroundColor Gray
            Write-Host ""
        }
    }
    
    "unrevoke" {
        if (-not $Code) {
            Write-Host "Error: specify -Code" -ForegroundColor Red
            return
        }
        
        $result = Invoke-Api "admin_unrevoke" @{ "code" = $Code }
        if ($result -and $result.success) {
            Write-Host ""
            Write-Host "=== REVOKE REMOVED ===" -ForegroundColor Green
            Write-Host "Code: $Code" -ForegroundColor Cyan
            Write-Host "Key is active again" -ForegroundColor Gray
            Write-Host ""
        }
    }
    
    "delete" {
        if (-not $Code) {
            Write-Host "Error: specify -Code" -ForegroundColor Red
            return
        }
        
        $result = Invoke-Api "admin_delete" @{ "code" = $Code }
        if ($result -and $result.success) {
            Write-Host ""
            Write-Host "=== KEY DELETED ===" -ForegroundColor Red
            Write-Host "Code: $Code" -ForegroundColor Cyan
            Write-Host "Key completely removed from database" -ForegroundColor Gray
            Write-Host ""
        }
    }
    
    "extend" {
        if (-not $Code) {
            Write-Host "Error: specify -Code" -ForegroundColor Red
            return
        }
        if ($Days -le 0) {
            Write-Host "Error: specify -Days (greater than 0)" -ForegroundColor Red
            return
        }
        
        $result = Invoke-Api "admin_extend" @{ "code" = $Code; "days" = $Days }
        if ($result -and $result.success) {
            Write-Host ""
            Write-Host "=== KEY EXTENDED ===" -ForegroundColor Green
            Write-Host "Code: $Code" -ForegroundColor Cyan
            Write-Host "Added: $Days days" -ForegroundColor Yellow
            Write-Host "New expiration: $($result.expires_date)" -ForegroundColor Gray
            Write-Host ""
        }
    }
    
    "reset_hwid" {
        if (-not $Code) {
            Write-Host "Error: specify -Code" -ForegroundColor Red
            return
        }
        
        $result = Invoke-Api "admin_reset_hwid" @{ "code" = $Code }
        if ($result -and $result.success) {
            Write-Host ""
            Write-Host "=== HWID RESET ===" -ForegroundColor Green
            Write-Host "Code: $Code" -ForegroundColor Cyan
            Write-Host "Player can activate on another PC" -ForegroundColor Gray
            Write-Host ""
        }
    }
}
